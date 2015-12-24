package se.gigurra.franklin.inmemimpl

import se.gigurra.franklin.Collection.Data
import se.gigurra.franklin.Item

/**
  * Created by johan on 2015-12-24.
  */
object Match {

  private def apply(pattern: Any, data: Any): Boolean = {
    data match {
      case data: Number => data == pattern
      case data: String => data == pattern
      case data: Boolean => data == pattern
      case data => ???
    }
  }

  private def apply(selector: Data,
                    data: Data,
                    actualVersion: Long,
                    expectVersion: Long): Result = {

    if (expectVersion != -1 && expectVersion != actualVersion) {
      WrongVersion
    } else {
      selector.forall {
        case (key, pattern) => data.get(key) match {
          case Some(dataValue) => apply(pattern, dataValue)
          case None => false
        }
      } match {
        case true => Correct
        case false => WrongPattern
      }
    }
  }

  def apply(selector: Data, item: Item, expectVersion: Long = -1L): Result = {
    apply(selector, item.data, item.version, expectVersion)
  }

  sealed trait Result
  case object Correct extends Result
  case object WrongVersion extends Result
  case object WrongPattern extends Result

}
