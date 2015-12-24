package se.gigurra.franklin

import scala.concurrent.Future
import Collection.Data

/**
  * Created by johan on 2015-12-23.
  */
trait Collection {

  def createIndex(fieldName: String, unique: Boolean = true): Future[Unit]
  def deleteIndex(fieldName: String): Future[Unit]

  def find(filter: Data): Future[Seq[Item]]

  def create(data: Data): Future[Unit]
  def update(filter: Data, data: Data, upsert: Boolean = false, expectVersion: Long = -1): Future[Unit]

  def loadOrCreate(filter: Data, ctor: () => Data): Future[Item]

  def append(filter: Data, values: Data): Future[Unit]

}

object Collection {
  type Data = Map[String, Any]
}

case class Item(data: Data, version: Long)
