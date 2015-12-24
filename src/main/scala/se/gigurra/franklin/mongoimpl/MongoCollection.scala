package se.gigurra.franklin.mongoimpl

import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.{Collection, Item}

import scala.concurrent.Future

/**
  * Created by johan on 2015-12-24.
  */
case class MongoCollection() extends Collection {
  override def createUniqueIndex(fieldName: String): Future[Unit] = ???
  override def update(selector: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] = ???
  override def create(data: Data): Future[Unit] = ???
  override def find(selector: Data): Future[Seq[Item]] = ???
  override def loadOrCreate(selector: Data, ctor: () => Data): Future[Item] = ???
  override def append(selector: Data, data: Data, defaultValue: () => Item): Future[Unit] = ???
}
