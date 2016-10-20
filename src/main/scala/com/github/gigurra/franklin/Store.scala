package com.github.gigurra.franklin

import java.io.Closeable

/**
  * Created by johan on 2015-12-23.
  */
trait Store extends Closeable {
  def getOrCreate(name: String): Collection
  def close(): Unit
}
