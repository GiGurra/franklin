package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.Item

import scala.reflect.ClassTag

/**
  * Created by johan on 2015-12-24.
  */
object Match {

  private def simple(selectorValue: Any, itemData: Any): Boolean = {
    (selectorValue, itemData) match {
      case (selectorValue: Iterable[Any], itemData: Iterable[Any]) =>
        selectorValue.toSet[Any].subsetOf(itemData.toSet[Any])
      case (selectorValue: Iterable[Any], itemData: Any) =>
        selectorValue.size == 1 && selectorValue.head == itemData
      case (selectorValue: Any, itemData: Iterable[Any]) =>
        itemData.toSet.contains(selectorValue)
      case (selectorValue, itemData) => selectorValue == itemData
    }
  }

  private def apply(selectorValue: Any, itemData: Any): Boolean = {
    simple(selectorValue, itemData)
  }

  private def apply(selector: Data,
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

  def apply(selector: Data, item: Item, expectVersion: Long = -1L): Result = {
    apply(selector, item.data, item.version, expectVersion)
  }

  sealed trait Result
  case object Correct extends Result
  case object WrongVersion extends Result
  case object WrongPattern extends Result

}
