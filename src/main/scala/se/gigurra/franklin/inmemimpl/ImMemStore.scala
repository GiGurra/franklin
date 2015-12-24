package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.{Collection, Store}

/**
  * Created by johan on 2015-12-24.
  */
case class ImMemStore() extends Store {
  override def getOrCreate(name: String): Collection = ???
  override def close(): Unit = ???
}
