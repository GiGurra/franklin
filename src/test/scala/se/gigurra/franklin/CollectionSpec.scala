package se.gigurra.franklin

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.inmemimpl.InMemCollection

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

class CollectionSpec
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest {

  implicit class RichFuture[T](f: Future[T]) {
    def await(): T = Await.result(f, Duration.Inf)
  }

  val store: Collection = InMemCollection()
  // val store: Collection = MongoCollection()

  "InMemCollection" should {

    "be created" in {
      store should not be null
    }

    "have some indices" in {
      store.createUniqueIndex("id").await()
      store.createUniqueIndex("woopie").await()
    }

    "add some items" in {

      val a = Map("id" -> "a")
      val b = Map("id" -> "b")

      store.create(a).await()

      val result0 = Try(store.create(b).await())
      result0 shouldBe an[Failure[_]]
      result0.failed.get shouldBe an[ItemAlreadyExists]

      store.createUniqueIndex("id").await()
      store.createUniqueIndex("woopie").await()
      store.create(b).await()
      Try(store.create(b).await()) shouldBe an[Failure[_]]
      Try(store.create(b).await()).failed.get shouldBe an[ItemAlreadyExists]

    }

    "find some items" in {

      val store = InMemCollection()
      store.createUniqueIndex("id").await()

      val a = Map("id" -> "a", "ouf" -> 123)
      val b = Map("id" -> "b", "bouf" -> "321")
      store.create(a).await()
      store.create(b).await()

      store.find("ouf" -> 123).await().head.data shouldBe a
      store.find("ouf" -> 123).await().head.data shouldBe a
      store.find().await().toSet shouldBe Set(Item(a), Item(b))
    }


  }
}
