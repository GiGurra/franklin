package se.gigurra.franklin.mongoimpl

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.DefaultBSONCommandError
import reactivemongo.api.commands.{CommandError, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong}
import reactivemongo.core.errors.DatabaseException
import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by johan on 2015-12-24.
  */
case class MongoCollection(collection: BSONCollection) extends Collection {

  import MongoCollection._

  override def createIndex(fieldName: String, unique: Boolean): Future[Unit] = {
    collection.indexesManager.ensure(Index(Seq(fieldName -> IndexType.Ascending), name = Some(fieldName), unique = unique)).map(_ => ())
  }

  override def update(selector: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] = {

    // Failure cases:
    // 1a. Passed wrong version (not found), upsert = false
    //  -> Just fails
    // 1b. Passed wrong version (not found), upsert = true
    //  -> Tries to create and fails on unique index check
    // 2a. Wrong pattern, upsert = false
    //  -> Just fails
    // 2b. Wrong pattern (not found), upsert = true
    //  -> Tries to create and fails on unique index check

    update(map2mongo(selector), map2mongo(data), upsert, expectVersion)
  }

  override def create(data: Data): Future[Unit] = {
    val raw = map2mongo(data)
    update(raw, raw, upsert = true, expectVersion = -1)
  }

  private def update(selectorWithoutVersion: BSONDocument, data: BSONDocument, upsert: Boolean, expectVersion: Long): Future[Unit] = {

    val mongoSelector =
      if (expectVersion != -1L) {
        selectorWithoutVersion.add(VERSION -> expectVersion)
      }
      else {
        selectorWithoutVersion
      }

    val mongoUpdate = BSONDocument("$set" -> data, "$inc" -> BSONDocument(VERSION -> 1L))

    writeOp(collection.update(mongoSelector, mongoUpdate, upsert = upsert), { n =>
      if (n <= 0) {
        size(selectorWithoutVersion).flatMap { n =>
          if (n > 0) {
            Future.failed(WrongDataVersion(s"Could not find the required item (v. $expectVersion) - another was found but with the wrong version for selector ${mongo2map(selectorWithoutVersion)}}"))
          } else {
            Future.failed(ItemNotFound(s"Could not find the required item (v. $expectVersion) to update for selector ${mongo2map(selectorWithoutVersion)}}"))
          }
        }
      } else {
        Future(())
      }
    }, { message =>
      if (upsert && expectVersion != -1L) {
        Future.failed(WrongDataVersion(s"upsert && expectVersion != -1L: Could not find the required item (v. $expectVersion) - another was found but with the wrong version for selector ${mongo2map(selectorWithoutVersion)}}, $message"))
      } else {
        if (upsert)
          Future.failed(ItemAlreadyExists(s"Can't create item (v. $expectVersion) for selector ${mongo2map(selectorWithoutVersion)}} as another item with the same unique keys exist: $message"))
        else
          Future.failed(ItemNotFound(s"Could not find the required item (v. $expectVersion) to update for selector ${mongo2map(selectorWithoutVersion)}}, $message"))
      }
    })
  }

  override def find(selector: Data): Future[Seq[Item]] = {
    val mongoSelector = map2mongo(selector)
    val query = collection.find(mongoSelector, BSONDocument("_id" -> 0))
    query.cursor[BSONDocument]().collect[Seq]().map(_.map(toItem))
  }

  override def wipeItems(): ItemsWiper = new ItemsWiper {
    override def yesImSure(): Future[Unit] = {
      writeOp(collection.remove(BSONDocument()), _ => Future(()), _ => Future(()))
    }
  }

  override def wipeIndices(): IndicesWiper = new IndicesWiper {
    override def yesImSure(): Future[Unit] = {
      indices.flatMap { indices =>
        val fs = indices.map(index => deleteIndex(index)(YeahReally()))
        Future.sequence(fs)
      }.map(_ => ())
    }
  }

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] = {
    find(selector).flatMap {
      case Seq() =>
        val data = ctor()
        update(selector, data, upsert = true).map(_ => Item(data))
      case items => Future.successful(items.head)
    }
  }

  override def size(selector: Data): Future[Int] = {
    size(map2mongo(selector))
  }

  override def append(selector: Data, defaultObject: () => Data, kv: Seq[(String, Iterable[Any])]): Future[Unit] = {

    def doAppend(): Future[Unit] = {
      val pushes = kv.map { case (k, v) => "$push" -> BSONDocument(k -> BSONDocument("$each" -> any2MongoValue(v))) }
      val updates = BSONDocument(pushes).add("$inc" -> BSONDocument(VERSION -> 1L))
      val mongoSelector = map2mongo(selector)

      writeOp(collection.update(mongoSelector, updates, upsert = false), _ => Future(()), x => Future.failed(GenericMongoError(x)))
    }

    contains(selector).flatMap {
      case true => doAppend()
      case false => update(selector, defaultObject(), upsert = true).flatMap(_ => doAppend())
    }
  }

  private def size(mongoSelector: BSONDocument): Future[Int] = {
    if (mongoSelector.isEmpty)
      collection.count()
    else
      collection.count(Some(mongoSelector))
  }

  private def writeOp(op: => Future[WriteResult],
                      updateCountResult: Int => Future[Unit],
                      uniqueConflictResult: String => Future[Unit]): Future[Unit] = {
    op.flatMap { result =>
      if (result.hasErrors) {
        val err = result.writeErrors.head
        Future.failed(DefaultBSONCommandError(Some(err.code), Some(err.errmsg), BSONDocument()))
      } else {
        updateCountResult(result.n)
      }
    }.recoverWith {
      case e: FranklinException => Future.failed(e)
      case e: DatabaseException if e.code.contains(11000) => uniqueConflictResult(e.getMessage())
      case e: CommandError if e.code.contains(11000) => uniqueConflictResult(e.getMessage())
      case e => Future.failed(GenericMongoError(e.getMessage, e))
    }
  }

  private def fromMongo(data: BSONDocument): Data = {
    mongo2map(data)
  }

  private def toItem(data: BSONDocument): Item = {
    val version = data.get(VERSION).map(_.asInstanceOf[BSONLong].value).getOrElse(-1L)
    Item(fromMongo(data.remove(VERSION)), version)
  }

  override def deleteItem(selector: Data, expectVersion: Long): Future[Unit] = {

    val selectorWithoutVersion = map2mongo(selector)

    val mongoSelector =
      if (expectVersion != -1L) {
        selectorWithoutVersion.add(VERSION -> expectVersion)
      } else {
        selectorWithoutVersion
      }

    writeOp(collection.remove(mongoSelector),
      updateCountResult = { n =>
        if (n <= 0) {
          size(selectorWithoutVersion).flatMap { n =>
            if (n > 0) {
              Future.failed(WrongDataVersion(s"Could not find the required item (v. $expectVersion) - another was found but with the wrong version for selector ${mongo2map(selectorWithoutVersion)}}"))
            } else {
              Future.failed(ItemNotFound(s"Could not find the required item (v. $expectVersion) to update for selector ${mongo2map(selectorWithoutVersion)}}"))
            }
          }
        } else {
          Future(())
        }
      },
      uniqueConflictResult = { _ => Future(()) } // Cannot happen on remove
    )

  }

  private def isMongoIdIndex(index: String): Boolean = {
    index == "_id" || index == "_id_"
  }

  override def deleteIndex(index: String)(yeahRly: YeahReally): Future[Unit] = {
    collection.indexesManager.drop(index).map(_ => ())
  }

  override def indices: Future[Seq[String]] = {
    collection.indexesManager.list().map(_.flatMap(_.name.toSeq)).map(_.filterNot(isMongoIdIndex))
  }
}

object MongoCollection {
  private val VERSION = "__version"

  case class GenericMongoError(message: String, cause: Throwable = null) extends FranklinException(message, cause)

}