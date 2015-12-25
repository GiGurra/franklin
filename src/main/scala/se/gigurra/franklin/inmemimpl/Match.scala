package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.Item

/**
  * Created by johan on 2015-12-24.
  */
object Match {

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

  private def matchesAnyOf(selector: Data,
                           data: Data,
                           actualVersion: Long,
                           expectVersion: Long): Result = {

    val out = if (expectVersion != -1 && expectVersion != actualVersion) {
      WrongVersion
    } else {
      selector.forall {
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

    out
  }

  private def matchAllOf(selector: Data,
                         data: Data,
                         actualVersion: Long,
                         expectVersion: Long): Result = {

    val out = if (expectVersion != -1 && expectVersion != actualVersion) {
      WrongVersion
    } else {
      selector.forall {
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

    out
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

  def apply(selector: Data, item: Item, uniqueIndices: Iterable[String], expectVersion: Long = -1L): Result = {
    if (matchAllOf(selector, item.data, item.version, expectVersion) == WrongVersion) {
      WrongVersion
    } else if (matchesAnyOf(project(selector, uniqueIndices), item.data) == Correct) {
      Correct
    } else {
      matchAllOf(selector, item.data, item.version, expectVersion)
    }
  }

  def project(selector: Data, fields: Iterable[String]): Data = synchronized {
    val projectFields = fields.toSet
    selector.filterKeys(projectFields.contains)
  }

  sealed trait Result

  case object Correct extends Result

  case object WrongVersion extends Result

  case object WrongPattern extends Result

}
