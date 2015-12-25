package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.inmemimpl.Match.{WrongVersion, WrongPattern, Correct}
import se.gigurra.franklin._
import se.gigurra.franklin.Collection.Data

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by johan on 2015-12-24.
  */
case class InMemCollection() extends Collection {

  val impl = InMemCollectionImpl()

  override def ensureUniqueIndex(fieldName: String): Future[Unit] =
    Future(impl.createUniqueIndex(fieldName))

  override def update(selector: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] =
    Future(impl.update(selector, data, upsert, expectVersion))

  override def append(selector: Data, defaultObject: () => Data, kv: Seq[(String, Iterable[Any])]): Future[Unit] = {
    Future(impl.append(selector, defaultObject, kv))
  }

  override def find(selector: Data): Future[Seq[Item]] =
    Future(impl.find(selector))

  override def create(data: Data): Future[Unit] =
    Future(impl.create(data))

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] =
    Future(impl.loadOrCreate(selector, ctor))

  override def size(selector: Data): Future[Int] =
    Future(impl.size(selector))

  override def wipe(): Wiper = new Wiper {
    override def yesImSure(): Future[Unit] = Future(impl.deleteAll())
  }
}

case class InMemCollectionImpl() {

  val storedData = new mutable.HashSet[Item]
  val uniqueIndices = new mutable.HashSet[String]

  def createUniqueIndex(fieldName: String): Unit = synchronized {
    uniqueIndices += fieldName
  }

  def update(selector: Data,
                  data: Data,
                  upsert: Boolean,
                  expectVersion: Long): Unit = synchronized {

    val matchResults =
      storedData
        .map(item => (item, Match(selector, item, expectVersion)))
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

          val newVersion = selected.map(_.version).max + 1
          selected.foreach(storedData -= _)

          Try(create(data, newVersion)) match {
            case Success(_) =>
            case Failure(e) =>
              selected.foreach(storedData += _)
              throw e
          }
        }
    }
  }

  def create(data: Data, version: Long = 1L): Unit = synchronized {
    val projected = project(data, uniqueIndices)
    find(projected) match {
      case Seq() => storedData += Item(data, version)
      case items => throw ItemAlreadyExists(s"Item already exists according to specified indices ($uniqueIndices), data: \n$data")
    }
  }

  def find(selector: Data): Seq[Item] = synchronized {
    storedData.filter(Match(selector, _) == Correct).toSeq
  }

  def loadOrCreate(selector: Data, ctor: () => Data): Item = synchronized {
    val projected = project(selector, uniqueIndices)
    find(projected) match {
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
             kv: Seq[(String, Iterable[Any])]): Unit = {
    find(selector) match {

      case Seq() =>
        create(Append(kv, defaultValue()))

      case items =>
        items.foreach { prevItem =>
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

  }


  def size(selector: Data): Int = {
    find(selector).size
  }

  def deleteAll(): Unit = {
    storedData.clear()

  }

  private def project(selector: Data, fields: Iterable[String]): Data = {
    val projectFields = fields.toSet
    selector.filterKeys(projectFields.contains)
  }

}