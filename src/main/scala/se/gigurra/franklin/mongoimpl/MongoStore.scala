package se.gigurra.franklin.mongoimpl

import se.gigurra.franklin.{Collection, Store}

/**
  * Created by johan on 2015-12-24.
  */
case class MongoStore() extends Store {
  override def getOrCreate(name: String): Collection = ???
  override def close(): Unit = ???
}
