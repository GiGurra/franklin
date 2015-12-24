package se.gigurra.franklin

import java.util.concurrent.TimeUnit

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.inmemimpl.InMemCollection

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Try}

class InMemSpec
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest {

  implicit class RichFuture[T](f: Future[T]) {
    def await(): T = Await.result(f, Duration.Inf)
  }

  "InMemCollection" should {

    "be created" in {
      val store = InMemCollection()
      store should not be null
    }

    "have some indices" in {
      val store = InMemCollection()
      store.createUniqueIndex("id")
      store.createUniqueIndex("woopie")
    }

    "add some items" in {
      val store = InMemCollection()

      val a = Map("id" -> "a")
      val b = Map("id" -> "b")

      store.create(a).await()
      store.storedData shouldBe Set(Item(a, 0))

      val result0 = Try(store.create(b).await())
      result0 shouldBe an[Failure[_]]
      result0.failed.get shouldBe an[ItemAlreadyExists]

      store.createUniqueIndex("id")
      store.createUniqueIndex("woopie")
      store.create(b).await()

      store.storedData shouldBe Set(Item(a, 0), Item(b, 0))

    }

  }
}
