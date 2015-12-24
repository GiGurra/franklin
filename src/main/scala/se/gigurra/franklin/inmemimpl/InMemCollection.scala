package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.{Item, Collection}
import se.gigurra.franklin.Collection.Data

import scala.concurrent.Future

/**
  * Created by johan on 2015-12-24.
  */
case class InMemCollection() extends Collection {
  override def createIndex(fieldName: String, unique: Boolean): Future[Unit] = ???
  override def update(filter: Data, data: Data, upsert: Boolean, expectVersion: Long): Future[Unit] = ???
  override def create(data: Data): Future[Unit] = ???
  override def find(filter: Data): Future[Seq[Item]] = ???
  override def deleteIndex(fieldName: String): Future[Unit] = ???
  override def loadOrCreate(filter: Data, ctor: () => Data): Future[Item] = ???
  override def append(filter: Data, values: Data): Future[Unit] = ???
}
