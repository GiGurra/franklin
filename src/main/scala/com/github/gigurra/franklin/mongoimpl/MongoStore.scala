package com.github.gigurra.franklin.mongoimpl

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import reactivemongo.api.{MongoConnection, DB}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import com.github.gigurra.franklin.{Collection, Store}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

/**
  * Created by johan on 2015-12-24.
  */
case class MongoStore(dbName: String, mongo: MongoDb, codec: BsonCodec) extends Store {

  implicit val timeout = Duration.apply(4, TimeUnit.SECONDS)
  private val logger = Logger.getLogger(this.getClass.getName)
  private val db = mongo.connection.apply(dbName)

  Try {
    // Check the connection
    val testCollection = db.apply[BSONCollection]("connection_test")
    val someOp = testCollection.insert(BSONDocument("abc" -> 123))
    Await.result(someOp, timeout)
  } match {
    case Success(_) =>
    case Failure(e) =>
      logger.severe("Failed to connect to mongodb - closing mongodb actorsystem")
      mongo.close()
      throw e
  }

  override def getOrCreate(name: String): Collection = {
    MongoCollection(db[BSONCollection](name), codec)
  }

  override def close(): Unit = {
    db.connection.actorSystem.shutdown()
  }
}
