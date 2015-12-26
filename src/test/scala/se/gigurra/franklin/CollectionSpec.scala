package se.gigurra.franklin

import java.util.UUID

import org.scalatest._
import org.scalatest.mock._
import se.gigurra.franklin.Collection._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class CollectionSpec
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  implicit class RichFuture[T](f: Future[T]) {
    def await(): T = Await.result(f, Duration.Inf)
  }

  val provider: Store = Franklin.loadInMemory()
  //val provider: Store = Franklin.loadMongo()

  val store: Collection = provider.getOrCreate("tests")

  override def beforeAll(): Unit = {
    store.wipeItems().yesImSure().await()
    store.wipeIndices().yesImSure().await()
  }

  override def afterAll(): Unit = {
    provider.close()
  }

  "InMemCollection" should {

    "be created" in {
      store should not be null
    }

    "create and delete unique indices" in {
      store.wipeItems().yesImSure().await()
      store.wipeIndices().yesImSure().await()

      store.createIndex("id", unique = true).await()
      store.indices.await() shouldBe Seq("id")
      store.createIndex("id1", unique = true).await()
      store.createIndex("id2", unique = true).await()
      store.createIndex("id3", unique = true).await()

      store.indices.await().toSet shouldBe Set("id", "id1", "id2", "id3")

      store.wipeIndices().yesImSure().await()
      store.indices.await() shouldBe empty
    }

    "create and delete non-unique indices" in {
      store.wipeItems().yesImSure().await()
      store.wipeIndices().yesImSure().await()

      store.createIndex("id", unique = false).await()
      store.indices.await() shouldBe Seq("id")
      store.createIndex("id1", unique = false).await()
      store.createIndex("id2", unique = false).await()
      store.createIndex("id3", unique = false).await()

      store.indices.await().toSet shouldBe Set("id", "id1", "id2", "id3")

      store.wipeIndices().yesImSure().await()
      store.indices.await() shouldBe empty
    }

    "add some items" in {

      store.createIndex("id", unique = true).await()

      val a1 = Map("id" -> "a", "somedata" -> 1)
      val a2 = Map("id" -> "a", "somedata" -> 2)

      store.create(a1).await()
      val resulta2 = Try(store.create(a2).await())
      resulta2 shouldBe an[Failure[_]]
      resulta2.failed.get shouldBe an[ItemAlreadyExists]

      val b1 = Map("id" -> "b", "somedata" -> 1)
      val b2 = Map("id" -> "b", "somedata" -> 2)
      store.create(b1).await()

      val result1 = Try(store.create(b2).await())
      result1 shouldBe an[Failure[_]]
      Try(store.create(b2).await()).failed.get shouldBe an[ItemAlreadyExists]

    }

    "find some items" in {

      store.createIndex("id", unique = true).await()

      val a = Map("id" -> "a", "ouf" -> 123)
      val b = Map("id" -> "b", "bouf" -> "321")
      store.create(a).await()
      store.create(b).await()

      store.where("ouf" -> 123).find.await().head.data shouldBe a
      store.where("ouf" -> 123).find.await().head.data shouldBe a
      store.where("bouf" -> "321").find.await().head.data shouldBe b
      store.where().find.await().toSet shouldBe Set(Item(a), Item(b))
    }

    "Update existing values" in {

      store.createIndex("id", unique = true).await()

      val a = Map("id" -> "a", "ouf" -> 123)
      val b = Map("id" -> "b", "bouf" -> "321")

      store.create(a).await()
      store.create(b).await()

      store.find("id" -> "a").await().head.data shouldBe a
      store.find("ouf" -> 321).await() shouldBe empty

      val updateWithWrongVersion =
        Try(store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 3321), expectVersion = 123L).await())

      updateWithWrongVersion shouldBe an[Failure[_]]
      updateWithWrongVersion.failed.get shouldBe an[WrongDataVersion]

      store.find("id" -> "a").await().head.data shouldBe a
      store.find("id" -> "b").await().head.data shouldBe b

      store.find("id" -> "a").await().head.version shouldBe 1L
      store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 321)).await()
      store.find("ouf" -> 321).await() should not be empty

      store.find("id" -> "a").await().head.data should not be a
      store.find("id" -> "a").await().head.data shouldBe Map("id" -> "a", "ouf" -> 321)

      // Check that version is incremented
      store.find("id" -> "a").await().head.version shouldBe 2L

      store.size().await() shouldBe 2
    }

    "Update existing values -> WrongPattern" in {

      store.createIndex("id", unique = true).await()

      val b = Map("id" -> "b", "bouf" -> "321")

      store.create(b).await()

      store.find("id" -> "a").await() shouldBe empty
      store.find("id" -> "b").await().size shouldBe 1

      val updateWithWrongPattern =
        Try(store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 3321), expectVersion = 3131).await())

      updateWithWrongPattern shouldBe an[Failure[_]]
      updateWithWrongPattern.failed.get shouldBe an[ItemNotFound]

    }

    "Update non-existing values" in {

      store.createIndex("id", unique = true).await()

      val a = Map("id" -> "a", "ouf" -> 123)

      val noUpsert = Try(store.where("id" -> "a").update(a, upsert = false).await())
      noUpsert shouldBe an[Failure[_]]
      noUpsert.failed.get shouldBe an[ItemNotFound]

      val upsert = Try(store.where("id" -> "a").update(a, upsert = true).await())
      upsert shouldBe an[Success[_]]

      val upsertRightVersion = Try(store.where("id" -> "a").update(a, upsert = true, expectVersion = 1L).await())
      upsertRightVersion shouldBe an[Success[_]]

      val upsertWrongVersion = Try(store.where("id" -> "a").update(a, upsert = true, expectVersion = 1L).await())
      upsertWrongVersion shouldBe an[Failure[_]]
      upsertWrongVersion.failed.get shouldBe an[WrongDataVersion]

      store.size().await() shouldBe 1
      store.isEmpty().await() shouldBe false
      store.nonEmpty().await() shouldBe true

      store.where().size.await() shouldBe 1
      store.where().isEmpty.await() shouldBe false
      store.where().nonEmpty.await() shouldBe true

    }

    "Index on arrays / find on index elements /Append" in {

      store.createIndex("id", unique = true).await()
      store.createIndex("ids", unique = true).await()

      val x = Map("id" -> "a", "ids" -> Seq(randomId))
      val y = Map("id" -> "b", "ids" -> Seq(randomId))

      store.create(x).await()
      store.create(y).await()


      store.where("id" -> "a").default(x).append("ids" -> Seq(1, 2, 3)).await()
      store.where("id" -> "a").default(x).append("ids" -> Seq(4, 5, 6)).await()
      store.where("id" -> "b").default(y).append("ids" -> Seq(7, 8, 9)).await()

      Try(store.where("id" -> "b").default(y).append("ids" -> Seq(1, 2, 4)).await()) shouldBe an[Failure[_]]

      store.where("id" -> "b").default(y).append("ids" -> Seq(11, 12, 13)).await()
      store.where().default(y).append("ids" -> Seq(21, 22, 23)).await()

      store.find("ids" -> 1).await().size shouldBe 1
      store.find("ids" -> 1).await().head.data.get("id") shouldBe Some("a")
      store.find("ids" -> 2).await().size shouldBe 1
      store.find("ids" -> 2).await().head.data.get("id") shouldBe Some("a")
      store.find("ids" -> 3).await().size shouldBe 1
      store.find("ids" -> 3).await().head.data.get("id") shouldBe Some("a")

      store.find("ids" -> 7).await().size shouldBe 1
      store.find("ids" -> 7).await().head.data.get("id") shouldBe Some("b")
      store.find("ids" -> 8).await().size shouldBe 1
      store.find("ids" -> 8).await().head.data.get("id") shouldBe Some("b")
      store.find("ids" -> 9).await().size shouldBe 1
      store.find("ids" -> 9).await().head.data.get("id") shouldBe Some("b")

    }

    "LoadOrCreate" in {
      store.createIndex("idx", unique = true)

      val x1 = store.where("idx" -> 1).default(Map("idx" -> 1, "name" -> "apan1", "yo" -> "da")).loadOrCreate.await()
      val x2 = store.where("idx" -> 2).default(Map("idx" -> 2, "name" -> "apan2", "yo" -> "da")).loadOrCreate.await()
      val x2b = store.where("idx" -> 2).default(Map("idx" -> 2, "name" -> "apan2b", "yo" -> "da")).loadOrCreate.await()

      store.find("idx" -> 1).await().head.data shouldBe Map("idx" -> 1, "name" -> "apan1", "yo" -> "da")
      store.find("idx" -> 2).await().head.data shouldBe Map("idx" -> 2, "name" -> "apan2", "yo" -> "da")
      store.size().await() shouldBe 2
    }

  }


  def randomId: String = UUID.randomUUID().toString
}
