package se.gigurra.franklin.mongoimpl

import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.concurrent.ExecutionContext.Implicits.global

case class MongoDb(driver: MongoDriver, connection: MongoConnection, codec: BsonCodec) {

  def getDb(dbName: String): MongoStore = new MongoStore(dbName, this, codec)

  def close(): Unit = {
    driver.system.shutdown()
  }
}

object MongoDb {

  def connect(nodeAddresses: Seq[String] = Seq("127.0.0.1:27017"), codec: BsonCodec): MongoDb = {

    val driver = new MongoDriver
    val connection = driver.connection(nodeAddresses)

    MongoDb(driver, connection, codec)

  }
}
