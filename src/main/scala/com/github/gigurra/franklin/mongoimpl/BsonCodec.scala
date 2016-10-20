package com.github.gigurra.franklin.mongoimpl

import java.time.Instant
import java.util.Date

import reactivemongo.bson._
import com.github.gigurra.franklin.FranklinException

trait BsonCodec {
  def mongoValue2Any(value: BSONValue): Any
  def any2MongoValue(value: Any): BSONValue

  def mongo2map(doc: BSONDocument): Map[String, Any] = {
    doc
      .elements
      .filter(_._1 != "_id")
      .toMap
      .map(pair => UnEscape(pair._1) -> mongoValue2Any(pair._2))
  }

  def map2mongo(map: Map[String, Any]): BSONDocument = {
    BSONDocument(map.map(pair => Escape(pair._1) -> any2MongoValue(pair._2)))
  }

}

case object DefaultBsonCodec extends BsonCodec {

  def mongoValue2Any(value: BSONValue): Any = {
    value match {
      case value: BSONArray => value.values.map(mongoValue2Any)
      case value: BSONBoolean => value.value
      case value: BSONDateTime => Instant.ofEpochMilli(BSONNumberLike.BSONDateTimeNumberLike(value).toLong)
      case value: BSONDocument => mongo2map(value)
      case value: BSONDouble => value.value
      case value: BSONInteger => value.value
      case value: BSONLong => value.value
      case value: BSONObjectID => value.stringify
      case value: BSONString => value.value
      case value: BSONTimestamp => Instant.ofEpochMilli(BSONNumberLike.BSONTimestampNumberLike(value).toLong)
      case BSONNull => null
      /*
      case value: BSONSymbol => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONSymbol]} to an Any")
      case value: BSONBinary => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONBinary]} to an Any")
      case value: BSONDBPointer => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONDBPointer]} to an Any")
      case value: BSONJavaScript => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONJavaScript]} to an Any")
      case value: BSONJavaScriptWS => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONJavaScriptWS]} to an Any")
      case BSONMaxKey => throw BsonCodecException(s"Don't know how to convert ${BSONMaxKey.getClass} to an Any")
      case BSONMinKey => throw BsonCodecException(s"Don't know how to convert ${BSONMinKey.getClass} to an Any")
      case value: BSONRegex => throw BsonCodecException(s"Don't know how to convert ${classOf[BSONRegex]} to an Any")
      case BSONUndefined => throw BsonCodecException(s"Don't know how to convert ${BSONUndefined.getClass} to an Any")
      */
      case x => throw BsonCodecException(s"DefaultBsonCodec:mongoValue2Any: Don't know how to convert $x to an Any")
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
      case x => throw BsonCodecException(s"DefaultBsonCodec:any2MongoValue: Don't know how to convert $x to a BSONValue")
    }
  }
}

case class BsonCodecException(message: String, cause: Throwable = null) extends FranklinException(message, cause)