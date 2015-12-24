package se.gigurra.franklin

import scala.util.Try

/**
  * Created by johan on 2015-12-24.
  */
object MongoLiveTest extends App {

  val mongo = Franklin.loadMongo()

  try {
    mongo.getOrCreate("test_users")

    Thread.sleep(1000)

  } finally {
    mongo.close()
  }
}
