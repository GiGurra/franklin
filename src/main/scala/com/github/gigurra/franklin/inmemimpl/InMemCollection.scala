package com.github.gigurra.franklin.inmemimpl

import com.github.gigurra.franklin.Collection.Data
import com.github.gigurra.franklin._
import com.github.gigurra.franklin.inmemimpl.Match.{Correct, WrongPattern, WrongVersion}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by johan on 2015-12-24.
  */
case class InMemCollection() extends Collection {

  val impl = InMemCollectionImpl()

  override def createIndex(fieldName: String, unique: Boolean): Future[Unit] =
    Future(impl.createIndex(fieldName, unique))

  override def update(selector: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] =
    Future(impl.update(selector, data, upsert, expectVersion))

  override def append(selector: Data, defaultObject: () => Data, kv: Seq[(String, Iterable[Any])]): Future[Unit] =
    Future(impl.append(selector, defaultObject, kv))

  override def find(selector: Data): Future[Seq[Item]] =
    Future(impl.find(selector))

  override def create(data: Data): Future[Unit] =
    Future(impl.create(data))

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] =
    Future(impl.loadOrCreate(selector, ctor))

  override def size(selector: Data): Future[Int] =
    Future(impl.size(selector))

  override def deleteItem(selector: Data, expectVersion: Long): Future[Unit] =
    Future(impl.deleteItem(selector, expectVersion))

  override def deleteIndex(index: String)(yeahRly: YeahReally): Future[Unit] =
    Future(impl.deleteIndex(index))

  override def indices: Future[Seq[String]] =
    Future(impl.indices)

  override def wipeItems(): ItemsWiper = new ItemsWiper {
    override def yesImSure(): Future[Unit] = Future(impl.deleteAllItems())
  }

  override def wipeIndices(): IndicesWiper = new IndicesWiper {
    override def yesImSure(): Future[Unit] = Future(impl.deleteAllIndices())
  }
}

case class InMemCollectionImpl() {

  val storedData = new mutable.HashSet[Item]
  val uniqueIndices = new mutable.HashSet[String]
  val performanceIndices = new mutable.HashSet[String]

  def createIndex(fieldName: String, unique: Boolean): Unit = synchronized {
    if (unique) {
      uniqueIndices += fieldName
    } else {
      performanceIndices += fieldName
    }
  }

  def update(selector: Data,
             data: Data,
             upsert: Boolean,
             expectVersion: Long): Unit = synchronized {

    val matchResults =
      storedData
        .map(item => (item, Match(selector, item, uniqueIndices, expectVersion)))
        .filter(_._2 != WrongPattern)

    matchResults.find(_._2 == WrongVersion) match {

      case Some(pair) =>
        throw WrongDataVersion(s"Wrong selected data version for update. Expect = $expectVersion, ")

      case None =>
        val selected = matchResults.map(_._1)

        if (selected.isEmpty) {
          if (upsert) {
            create(data)
          } else {
            throw ItemNotFound(s"Couldn't find item for $selector")
          }
        } else {

          val prevItem = selected.head
          val newVersion = prevItem.version + 1
          storedData -= prevItem

          Try(create(data, newVersion)) match {
            case Success(_) =>
            case Failure(e) =>
              storedData += prevItem
              throw e
          }
        }
    }
  }

  def create(data: Data, version: Long = 1L): Unit = synchronized {
    find(data, matchOnAnyUnique = true) match {
      case Seq() => storedData += Item(data, version)
      case items => throw ItemAlreadyExists(s"Item already exists according to specified indices ($uniqueIndices), data: \n$data")
    }
  }

  def find(selector: Data, matchOnAnyUnique: Boolean = false): Seq[Item] = synchronized {
    storedData.filter(Match(selector, _, if (matchOnAnyUnique) uniqueIndices else Set.empty) == Correct).toSeq
  }

  def loadOrCreate(selector: Data, ctor: () => Data): Item = synchronized {
    find(selector) match {
      case Seq() =>
        val data = ctor()
        create(data, version = 1)
        Item(data, version = 1)

      case items =>
        items.head
    }
  }

  def append(selector: Data,
             defaultValue: () => Data,
             kv: Seq[(String, Iterable[Any])]): Unit = synchronized {
    find(selector) match {

      case Seq() =>
        create(Append(kv, defaultValue()))

      case items =>
        val prevItem = items.head
        val version = prevItem.version + 1
        storedData -= prevItem
        Try(create(Append(kv, prevItem.data), version)) match {
          case Success(_) =>
          case Failure(e) =>
            storedData += prevItem
            throw e
        }
    }

  }


  def size(selector: Data): Int = synchronized {
    find(selector).size
  }

  def deleteAllItems(): Unit = synchronized {
    storedData.clear()
  }

  def deleteAllIndices(): Unit = synchronized {
    uniqueIndices.clear()
    performanceIndices.clear()
  }

  def deleteItem(selector: Data, expectVersion: Long): Unit = synchronized {

    def deleteSingleItem(item: Item): Unit = {
      if (expectVersion != -1 && expectVersion != item.version) {
        throw WrongDataVersion(s"Cannot delete item of expected version ${expectVersion}, when actual version is ${item.version}")
      } else {
        storedData -= item
      }
    }

    find(selector) foreach deleteSingleItem
  }

  def deleteIndex(index: String): Unit = {
    uniqueIndices -= index
    performanceIndices -= index
  }

  def indices: Seq[String] = synchronized {
    (uniqueIndices ++ performanceIndices).toArray.toSeq
  }

}