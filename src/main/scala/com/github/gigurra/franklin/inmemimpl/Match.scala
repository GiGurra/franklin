package com.github.gigurra.franklin.inmemimpl

import com.github.gigurra.franklin.Collection.Data
import com.github.gigurra.franklin.Item

/**
  * Created by johan on 2015-12-24.
  */
object Match {

  def apply(selector: Data, item: Item, uniqueIndices: Iterable[String], expectVersion: Long = -1L): Result = {
    matchAllOf(selector, item.data, item.version, expectVersion) match {
      case WrongVersion => WrongVersion
      case Correct => Correct
      case WrongPattern => matchesAnyOf(project(selector, uniqueIndices), item.data)
    }
  }

  sealed trait Result
  case object Correct extends Result
  case object WrongVersion extends Result
  case object WrongPattern extends Result

  private def simple(selectorValue: Any, itemData: Any): Boolean = {
    (selectorValue, itemData) match {
      case (selectorValue: Iterable[Any], itemData: Iterable[Any]) =>
        val itemDataSet = itemData.toSet
        selectorValue.exists(itemDataSet.contains)
      case (selectorValue: Iterable[Any], itemData: Any) =>
        selectorValue.toSet.contains(itemData)
      case (selectorValue: Any, itemData: Iterable[Any]) =>
        itemData.toSet.contains(selectorValue)
      case (selectorValue, itemData) => selectorValue == itemData
    }
  }

  private def apply(selectorValue: Any, itemData: Any): Boolean = {
    simple(selectorValue, itemData)
  }

  private def matchAllOf(selector: Data,
                         data: Data,
                         actualVersion: Long,
                         expectVersion: Long): Result = {

    selector.forall {
      case (selectorKey, selectorValue) =>
        data.get(selectorKey) match {
          case Some(dataValue) => apply(selectorValue, dataValue)
          case None => false
        }
    } match {
      case true => if (expectVersion != -1 && expectVersion != actualVersion) {
        WrongVersion
      } else {
        Correct
      }
      case false => WrongPattern
    }
  }

  private def matchesAnyOf(selector: Data, data: Data): Result = {
    selector.exists {
      case (selectorKey, selectorValue) =>
        data.get(selectorKey) match {
          case Some(dataValue) => apply(selectorValue, dataValue)
          case None => false
        }
    } match {
      case true => Correct
      case false => WrongPattern
    }
  }

  private def project(selector: Data, fields: Iterable[String]): Data = synchronized {
    val projectFields = fields.toSet
    selector.filterKeys(projectFields.contains)
  }


}
