package se.gigurra.franklin

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.inmemimpl.InMemCollection

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Success, Failure, Try}

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

      store.createUniqueIndex("id").await()

      val a = Map("id" -> "a", "ouf" -> 123)
      val b = Map("id" -> "b", "bouf" -> "321")
      store.create(a).await()
      store.create(b).await()

      store.where("ouf" -> 123).find.await().head.data shouldBe a
      store.where("ouf" -> 123).find.await().head.data shouldBe a
      store.where().find.await().toSet shouldBe Set(Item(a), Item(b))
    }

    "Update existing values" in {

      store.createUniqueIndex("id").await()

      val a = Map("id" -> "a", "ouf" -> 123)
      val b = Map("id" -> "b", "bouf" -> "321")

      store.create(a).await()
      store.create(b).await()

      store.find("id" -> "a").await().head.data shouldBe a
      store.find("ouf" -> 321).await() shouldBe empty

      val updateWithWrongVersion = Try(store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 3321), expectVersion = 123L).await())
      updateWithWrongVersion shouldBe an[Failure[_]]
      updateWithWrongVersion.failed.get shouldBe an [WrongDataVersion]

      store.find("id" -> "a").await().head.data shouldBe a
      store.find("id" -> "b").await().head.data shouldBe b

      store.find("id" -> "a").await().head.version shouldBe 0L
      store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 321)).await()
      store.find("ouf" -> 321).await() should not be empty

      store.find("id" -> "a").await().head.data should not be a
      store.find("id" -> "a").await().head.data shouldBe Map("id" -> "a", "ouf" -> 321)

      // Check that version is incremented
      store.find("id" -> "a").await().head.version shouldBe 1L

      store.size().await() shouldBe 2

    }

    "Update non-existing values" in {

      store.createUniqueIndex("id").await()

      val a = Map("id" -> "a", "ouf" -> 123)

      val noUpsert = Try(store.where("id"-> "a").update(a, upsert = false).await())
      noUpsert shouldBe an [Failure[_]]
      noUpsert.failed.get shouldBe an[ItemNotFound]

      val upsert = Try(store.where("id"-> "a").update(a, upsert = true).await())
      upsert shouldBe an [Success[_]]

      val upsertRightVersion = Try(store.where("id"-> "a").update(a, upsert = true, expectVersion = 0L).await())
      upsertRightVersion shouldBe an [Success[_]]

      val upsertWrongVersion = Try(store.where("id"-> "a").update(a, upsert = true, expectVersion = 0L).await())
      upsertWrongVersion shouldBe an [Failure[_]]
      upsertWrongVersion.failed.get shouldBe an[WrongDataVersion]

      store.size().await() shouldBe 1
      store.isEmpty().await() shouldBe false
      store.nonEmpty().await() shouldBe true

      store.where().size.await() shouldBe 1
      store.where().isEmpty.await() shouldBe false
      store.where().nonEmpty.await() shouldBe true

    }


  }
}
