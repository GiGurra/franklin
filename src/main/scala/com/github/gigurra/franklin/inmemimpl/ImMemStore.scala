package com.github.gigurra.franklin.inmemimpl

import com.github.gigurra.franklin.{Collection, Store}

import scala.collection.mutable

/**
  * Created by johan on 2015-12-24.
  */
case class ImMemStore() extends Store {

  private val collections = new mutable.HashMap[String, Collection]()

  override def getOrCreate(name: String): Collection = synchronized {
    collections.getOrElseUpdate(name, new InMemCollection)
  }

  override def close(): Unit = {
  }
}
