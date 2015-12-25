package se.gigurra.franklin.mongoimpl

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.DefaultBSONCommandError
import reactivemongo.api.commands.{WriteError, CommandError, WriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONLong, BSONDocument}
import reactivemongo.core.errors.DatabaseException
import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by johan on 2015-12-24.
  */
case class MongoCollection(collection: BSONCollection) extends Collection {
  import MongoCollection._

  override def ensureUniqueIndex(fieldName: String): Future[Unit] = {
    collection.indexesManager.ensure(Index(Seq(fieldName -> IndexType.Ascending), unique = true)).map(_ => ())
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
            Future.failed(WrongDataVersion(s"Could not find the required item (v. ${expectVersion}) - another was found but with the wrong version for selector ${selectorWithoutVersion}}"))
          } else {
            Future.failed(ItemNotFound(s"Could not find the required item (v. ${expectVersion}) to update for selector ${selectorWithoutVersion}}"))
          }
        }
      } else {
        Future(())
      }
    })
  }

  override def find(selector: Data): Future[Seq[Item]] = {
    val mongoSelector = map2mongo(selector)
    val query = collection.find(mongoSelector, BSONDocument("_id" -> 0))
    query.cursor[BSONDocument]().collect[Seq]().map(_.map(toItem))
  }

  override def wipe(): Wiper = new Wiper {
    override def yesImSure(): Future[Unit] = {
      writeOp(collection.remove(BSONDocument()), _ => Future(()))
    }
  }

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] = ???

  override def size(selector: Data): Future[Int] = {
    size(map2mongo(selector))
  }

  override def append(selector: Data, defaultObject: () => Data, kv: Seq[(String, Iterable[Any])]): Future[Unit] = {
    // Append + inc version
    //
    ???
  }

  private def size(mongoSelector: BSONDocument): Future[Int] = {
    if (mongoSelector.isEmpty)
      collection.count()
    else
      collection.count(Some(mongoSelector))
  }

  private def writeOp(op: => Future[WriteResult], updateCountResult: Int => Future[Unit]): Future[Unit] = {
    op.flatMap { result =>
      if (result.hasErrors) {
        val err = result.writeErrors.head
        Future.failed(DefaultBSONCommandError(Some(err.code), Some(err.errmsg), BSONDocument()))
      } else {
        updateCountResult(result.n)
      }
    }.recoverWith {
      case e: DatabaseException if e.code.contains(11000) =>
        Future.failed(ItemAlreadyExists(s"An item with conflicting unique indices already exists in db (${e.getMessage()})", e))
      case e: CommandError if e.code.contains(11000) =>
        Future.failed(ItemAlreadyExists(s"An item with conflicting unique indices already exists in db (${e.getMessage()})", e))
      case e =>
        Future.failed(Error(e.getMessage, e))
    }
  }

  /*
  private def toMongoWithoutIncVersion(data: Data): BSONDocument = {
    map2mongo(data)
  }

  private def toMongoWithIncversion(data: Data): BSONDocument = {
    toMongoWithoutIncVersion(data).add(BSONDocument("$inc" -> BSONDocument(VERSION -> 1L)))
  }*/

  private def fromMongo(data: BSONDocument): Data = {
    mongo2map(data)
  }

  private def toItem(data: BSONDocument): Item = {
    val version = data.get(VERSION).map(_.asInstanceOf[BSONLong].value).getOrElse(-1L)
    Item(fromMongo(data.remove(VERSION)), version)
  }

}

object MongoCollection {
  private val VERSION = "__version"
  case class Error(message: String, cause: Throwable = null) extends FranklinException(message, cause)
}