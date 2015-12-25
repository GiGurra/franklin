package se.gigurra.franklin

import java.time.Instant
import java.util.Date

import reactivemongo.bson._

/**
  * Created by kjolh on 12/25/2015.
  */
package object mongoimpl {

  def mongoValue2Any(value: BSONValue): Any = {
    value match {
      case value: BSONArray => value.values.toSeq.map(mongoValue2Any)
      case value: BSONBinary => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONBinary]} to an Any")
      case value: BSONBoolean => value.value
      case value: BSONDBPointer => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONDBPointer]} to an Any")
      case value: BSONDateTime => Instant.ofEpochMilli(BSONNumberLike.BSONDateTimeNumberLike(value).toLong)
      case value: BSONDocument => mongo2map(value)
      case value: BSONDouble => value.value
      case value: BSONInteger => value.value
      case value: BSONJavaScript => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONJavaScript]} to an Any")
      case value: BSONJavaScriptWS => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONJavaScriptWS]} to an Any")
      case value: BSONLong => value.value
      case BSONMaxKey => throw new Mongo2MapException(s"Don't know how to convert ${BSONMaxKey.getClass} to an Any")
      case BSONMinKey => throw new Mongo2MapException(s"Don't know how to convert ${BSONMinKey.getClass} to an Any")
      case BSONNull => null
      case value: BSONObjectID => value.stringify
      case value: BSONRegex => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONRegex]} to an Any")
      case value: BSONString => value.value
      case value: BSONSymbol => throw new Mongo2MapException(s"Don't know how to convert ${classOf[BSONSymbol]} to an Any")
      case value: BSONTimestamp => Instant.ofEpochMilli(BSONNumberLike.BSONTimestampNumberLike(value).toLong)
      case BSONUndefined => throw new Mongo2MapException(s"Don't know how to convert ${BSONUndefined.getClass} to an Any")
      case x => throw new Mongo2MapException(s"Don't know how to convert $x to an Any")
    }
  }

  def any2MongoValue(value: Any): BSONValue = {
    value match {
      case value: Byte => BSONInteger(value)
      case value: Short => BSONInteger(value)
      case value: Int => BSONInteger(value)
      case value: Long => BSONLong(value)
      case value: BigInt => BSONLong(value.longValue())
      case value: Date => BSONDateTime.apply(value.getTime)
      case value: Instant => BSONDateTime.apply(value.toEpochMilli)
      case value: java.math.BigInteger => BSONLong(value.longValue())
      case value: String => BSONString(value)
      case value: Map[_, _] => map2mongo(value.asInstanceOf[Map[String, Any]])
      case value: Iterable[_] => BSONArray(value.map(any2MongoValue))
      case value: Boolean => BSONBoolean(value)
      case null => BSONNull
      case x => throw new Mongo2MapException(s"Don't know how to convert $x to a BSONValue")
    }
  }

  def mongo2map(doc: BSONDocument): Map[String, Any] = {
    doc
      .elements
      .filter(_._1 != "_id")
      .map(pair => pair._1 -> mongoValue2Any(pair._2))
      .toMap
  }

  def map2mongo(map: Map[String, Any]): BSONDocument = {
    BSONDocument(map
      .map(pair => pair._1 -> any2MongoValue(pair._2))
      .toSeq
    )
  }

  case class Mongo2MapException(message: String, cause: Throwable = null) extends FranklinException(message, cause)
}
