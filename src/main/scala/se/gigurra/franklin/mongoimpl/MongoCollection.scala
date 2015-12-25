package se.gigurra.franklin.mongoimpl

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.{Collection, FranklinException, Item}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by johan on 2015-12-24.
  */
case class MongoCollection(collection: BSONCollection) extends Collection {
  import MongoCollection._

  override def createUniqueIndex(fieldName: String): Future[Unit] = {
    collection.indexesManager.ensure(Index(Seq(fieldName -> IndexType.Text), unique = true)).map(_ => ())
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

    val mongoSelector =
      if (expectVersion != -1)
        toMongoWithoutIncVersion(selector).add(VERSION -> expectVersion)
      else
        toMongoWithoutIncVersion(selector)

    val mongoData = toMongoWithIncversion(data)

    writeOp(collection.update(mongoSelector, mongoData, upsert = upsert))
  }

  override def create(data: Data): Future[Unit] = {
    val mongoData = toMongoWithIncversion(data)
    writeOp(collection.insert(mongoData))
  }

  override def find(selector: Data): Future[Seq[Item]] = {
    val mongoSelector = toMongoWithoutIncVersion(selector)
    val query = collection.find(mongoSelector, BSONDocument("_id" -> 0))
    query.cursor[BSONDocument]().collect[Seq]().map(_.map(toItem))
  }

  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] = ???

  override def size(selector: Data): Future[Int] = ???

  override def append(selector: Data, defaultObject: () => Data, kv: Seq[(String, Iterable[Any])]): Future[Unit] = {
    // Append + inc version
    //
    ???
  }

  private def writeOp(op: => Future[WriteResult]): Future[Unit] = {
    op.flatMap { result =>
      if (result.hasErrors)
        Future.failed(Error(result.message))
      else
        Future.successful(())
    }.recoverWith {
      case e =>
        println(e)
        Future.failed(Error(e.getMessage, e))
    }
  }

  private def toMongoWithoutIncVersion(data: Data): BSONDocument = {
    map2mongo(data)
  }

  private def toMongoWithIncversion(data: Data): BSONDocument = {
    toMongoWithoutIncVersion(data).add("$inc" -> BSONDocument(VERSION -> 1))
  }

  private def fromMongo(data: BSONDocument): Data = {
    ???
  }

  private def toItem(data: BSONDocument): Item = {
    val version = data.get(VERSION).map(_.asInstanceOf[Long]).getOrElse(-1L)
    Item(fromMongo(data.remove(VERSION)), version)
  }

}

object MongoCollection {
  private val VERSION = "__version"
  case class Error(message: String, cause: Throwable = null) extends FranklinException(message, cause)
}