package se.gigurra.franklin.mongoimpl

import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.{Collection, Item}

import scala.concurrent.Future

/**
  * Created by johan on 2015-12-24.
  */
case class MongoCollection() extends Collection {
  override def createIndex(fieldName: String, unique: Boolean): Future[Unit] = ???
  override def update(filter: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] = ???
  override def create(data: Data): Future[Unit] = ???
  override def find(filter: Data): Future[Seq[Item]] = ???
  override def deleteIndex(fieldName: String): Future[Unit] = ???
  override def loadOrCreate(filter: Data, ctor: () => Data): Future[Item] = ???
  override def append(filter: Data, values: Data): Future[Unit] = ???
}
