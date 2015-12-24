package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.inmemimpl.Match.{WrongVersion, WrongPattern, Correct}
import se.gigurra.franklin._
import se.gigurra.franklin.Collection.Data

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

import ExecutionContext.Implicits.global

/**
  * Created by johan on 2015-12-24.
  */
case class InMemCollection() extends Collection {

  private val storedData = new mutable.HashSet[Item]
  private val uniqueIndices = new mutable.HashSet[String]

  override def createUniqueIndex(fieldName: String): Future[Unit] = synchronized {
    uniqueIndices += fieldName
    Future.successful(())
  }

  override def update(selector: Data,
                      data: Data,
                      upsert: Boolean,
                      expectVersion: Long): Future[Unit] = synchronized {

    val matchResults =
      storedData
        .map(item => (item, Match(selector, item, expectVersion)))
        .filterNot(_._2 != WrongPattern)

    matchResults.find(_._2 == WrongVersion) match {

      case Some(pair) =>
        Future.failed(WrongDataVersion(s"Wrong selected data version for update. Expect = $expectVersion, "))

      case None =>
        val selected = matchResults.map(_._1)

        selected.foreach { item =>
          storedData -= item
        }

        if (selected.isEmpty) {
          if (upsert) {
            storedData += Item(data, version = 0)
            Future.successful(())
          } else {
            Future.failed(ItemNotFound(s"Couldn't find item for $selector"))
          }
        } else {
          val newVersion = selected.map(_.version).max + 1
          storedData += Item(data, newVersion)
          Future.successful(())
        }

    }

  }

  override def create(data: Data): Future[Unit] = synchronized {
    val projected = project(data, uniqueIndices)
    find(projected).flatMap {
      case Seq() => Future.successful(storedData += Item(data, version = 0))
      case items => Future.failed(ItemAlreadyExists(s"Item already exists according to specified indices ($uniqueIndices), data: \n$data"))
    }
  }

  override def find(selector: Data): Future[Seq[Item]] = synchronized {
    Future.successful(storedData.filter(Match(selector, _) == Correct).toSeq)
  }

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] = synchronized {
    val projected = project(selector, uniqueIndices)
    find(projected).flatMap {
      case Seq() => Future.successful {
        val item = Item(ctor(), version = 0)
        storedData += item
        item
      }
      case items =>
        Future.successful(items.head)
    }
  }

  override def append(selector: Data, data: Data, defaultValue: () => Item): Future[Unit] = synchronized {
    find(selector).flatMap {
      case Seq() => Future.successful {
        storedData += Append.apply(data, defaultValue())
      }
      case items => Future.successful {
        storedData -= items.head
        storedData += Append(data, items.head)
      }
    }
  }

  private def project(selector: Data, fields: Iterable[String]): Data = {
    val projectFields = fields.toSet
    selector.filterKeys(projectFields.contains)
  }

}
