package com.github.gigurra.franklin.inmemimpl

import com.github.gigurra.franklin.Collection.Data

/**
  * Created by johan on 2015-12-24.
  */
object Append {

  def apply(newData: Seq[(String, Iterable[Any])], prevData: Data): Data = {
    newData.foldLeft(prevData) { (item, kv) => {
      val key = kv._1
      val value = kv._2
      value match {
        case value: Iterable[_] => apply(key, value, item)
        case value => apply(key, Seq(value), item)
      }
    }}
  }

  private def apply(key: String, values: Iterable[Any], item: Data): Data = {
    val newArrayData = item.get(key) match {
      case Some(prevValues: Iterable[_]) => prevValues ++ values
      case Some(prevValue) => prevValue +: values.toSeq
      case None => values
    }

    item + (key -> newArrayData)
  }
}
