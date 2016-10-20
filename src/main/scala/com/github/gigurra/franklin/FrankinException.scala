package com.github.gigurra.franklin

/**
  * Created by johan on 2015-12-24.
  */
abstract class FranklinException(message: String, cause: Throwable)
  extends RuntimeException(message, cause)
