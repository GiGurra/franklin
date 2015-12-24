package se.gigurra.franklin

import Collection.Data
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global


/**
  * Created by johan on 2015-12-23.
  */
trait Collection {

  def createUniqueIndex(fieldName: String): Future[Unit]

  def find(selector: Data): Future[Seq[Item]]

  def create(data: Data): Future[Unit]
  def loadOrCreate(selector: Data, ctor: () => Data): Future[Item]

  def update(selector: Data, data: Data, upsert: Boolean = false, expectVersion: Long = -1L): Future[Unit]
  def append(selector: Data, data: Data, defaultValue: () => Data): Future[Unit]

  def size(selector: Data): Future[Int]

  
  ///////////////////////////
  // Convenience methods

  case class where(statements: (String, Any)*) {
    val selector = statements.toMap

    def find: Future[Seq[Item]] = Collection.this.find(selector)
    def update(data: Data, upsert: Boolean = false, expectVersion: Long = -1L): Future[Unit] = Collection.this.update(selector, data, upsert, expectVersion)
    def loadOrCreate(ctor: () => Data): Future[Item] = Collection.this.loadOrCreate(selector, ctor)
    def append(data: Data, defaultValue: () => Data): Future[Unit] = Collection.this.append(selector, data, defaultValue)
    def append(field: String, items: Seq[Any], defaultValue: () => Data): Future[Unit] = Collection.this.append(selector, Map(field -> items), defaultValue)
    def size: Future[Int] = Collection.this.size(statements.toMap)
    def isEmpty: Future[Boolean] = Collection.this.isEmpty(statements.toMap)
    def nonEmpty: Future[Boolean] = Collection.this.nonEmpty(statements.toMap)
  }

  def isEmpty(selector: Data): Future[Boolean] = size(selector).map(_ == 0)
  def nonEmpty(selector: Data): Future[Boolean] = size(selector).map(_ != 0)

  def size(statements: (String, Any)*): Future[Int] = size(statements.toMap)
  def find(statements: (String, Any)*): Future[Seq[Item]] = find(statements.toMap)
  def isEmpty(statements: (String, Any)*): Future[Boolean] = isEmpty(statements.toMap)
  def nonEmpty(statements: (String, Any)*): Future[Boolean] = nonEmpty(statements.toMap)

}

object Collection {
  type Data = Map[String, Any]
}

case class Item(data: Data, version: Long = 0)

case class UndefinedBehaviour(message: String, cause: Throwable = null)
  extends CollectionException(message, cause)

case class WrongDataVersion(message: String, cause: Throwable = null)
  extends CollectionException(message, cause)

case class ItemNotFound(message: String, cause: Throwable = null)
  extends CollectionException(message, cause)

case class ItemAlreadyExists(message: String, cause: Throwable = null)
  extends CollectionException(message, cause)

abstract class CollectionException(message: String, cause: Throwable)
  extends FranklinException(message, cause)