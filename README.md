# Franklin

.. stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing.  Franklin keeps a version number on each item stored in his collections - He is CAS capable and if a lot of people try to modify the same item at the *exact* same time, he will only let one of you do it. Every time an item is updated - its version number is automatically incremented.

Franklin is just a wrapper for a subset of ReactiveMongo with the option to run in-memory instead of against a mongodb database. Franklin gets a few new APIs every now and then. Franklin's APIs are asynchronous - every operation returns in a future, which you can run  in parallell or sequence (e.g. in a for comprehension or async-await, whatever floats your boat) and/or fork/join at any point.

Franklin was initially created to support [valhalla-game](https://github.com/saiaku-gaming/valhalla-server) - and has been extended and tested as needed. Go ahead and use it for whatever purpose you want (MIT licensed) .. if you want to :). 

Also see [Franklin-Heisenberg](https://github.com/gigurra/franklin-heisenberg-bridge)

Franklin doesn't do any kind of authentication, so you better use him on a secure net or only talk to mongo on loopback!


## Examples

### What you need

In your build.sbt:
```sbt
.dependsOn(uri("git://github.com/GiGurra/franklin.git#0.1.7"))
```
In your code:
```scala
import se.gigurra.franklin._

val provider: Store = Franklin.loadInMemory()
 // or Franklin.loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017"))

```

### Create a collection

```scala
val collection: Collection = provider.getOrCreate("test_document")
// If you want to add some more mongodb magic you can cast this to a 
// *case class MongoCollection(collection: BSONCollection)*
// and access the underlying reactivemongo collection directly.
```

### Create some indices

```scala
val op1: Future[Unit] = collection.createIndex("uuid", unique = true)
val op2: Future[Unit] = collection.createIndex("items", unique = false)
```

### Create some documents

```scala

// Assuming having previously done:
// collection.createIndex("id", unique = true)

val a = Map("id" -> "a", "somedata" -> 1)
val b = Map("id" -> "b", "somedata" -> 1)

val aOp: Future[Unit] = collection.create(a)
val bOp: Future[Unit] = collection.create(b)

// Trying to create the same document again
// with a unique index conflict will eventually
// complete the returned future with an 
// ItemAlreadyExists exception.
val bOp2: Future[Unit] = collection.create(b)

```

### Find some documents

```scala

// Either access the data with raw 'select-like' statements/filters
val data1: Future[Seq[Item]] = collection.where("id" -> "a", "somedata" -> 1).find

// Or pass in a map
val query = Map("id" -> "a")
val data2: Future[Seq[Item]] = collection.where(query).find

// An Item is:
// case class Item(data: Map[String, Any], version: Long)

```

### Update some documents

Per document updates completely replace the previous content. They are atomic - per mongodb design (as is the franklin in-memory implementation). All updates respect unique index constraints or return failing futures.

```scala

// You specify the item to replace, its new data, if upsert (create new if missing), and the expected version.
// The expectVersion is the version you expect the previously stored data to have. If you specify the wrong version,
// The returned future will eventually complete with an exception (probably a WrongDataVersion exception)
// The starting version number when they're first created in the database is 1
val op1: Future[Unit] = collection.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 3321), upsert = false, expectVersion = 3)

// To ignore the data version & invite race conditions - omit the expectVersion parameter or set it to -1
val op2: Future[Unit] = collection.where("id" -> "b").update(Map("id" -> "b", "ouf" -> 123))

```

### Append some data

You can also append data atomically to a document without having to replace the entire document. This is currently implemented in Franklin for Seq[..] and Set[..] fields. If you want more advanced append logic you can always throw me a pull request or just store the appended data in an entirely new document and use indexing or performance.

Appended entries respect unique index constraints and will return failing futures if the required conditions are not met. The *default* is an expression from () => Map[String, Any] (or just pass by name expr => .., Franklin supports either)  used when no document matching your search criteria exists.

Version numbers are currently not supported for append operations, but append operations themselves are atomic and will complete without destroying any previous or concurrent modifications.

```scala

// Assuming somewhere previously
// collection.createIndex("id", unique = true)
// collection.createIndex("ids", unique = true)

val op1: Future[Unit] = collection.where("id" -> "a").default(a).append("ids" -> Seq(1, 2, 3))
val op2: Future[Unit] = collection.where("id" -> "b").default(b).append("ids" -> Seq(4, 5, 6))
// Will fail since the "ids" field is uniquely indexed and the "id"->"a" document's "ids"
// field contains one or more of these elements. Operations are atomic and completed entirely
// or not at all, so '99' will NOT be added to the "id -> "b" document
val op3: Future[Unit] = collection.where("id" -> "b").default(b).append("ids" -> Seq(99, 1, 2))

```

### Atomic load-or-create

```scala
// *default* works the same as it does for append operations
val op: Future[Item] = collection.where("id" -> "a").default(Map("id" -> "a", "name" -> "monkey", "yo" -> "da")).loadOrCreate
```


### Wipe everything

Note: These operations are NOT atomic

```scala
 val op1: Future[Unit] = collection.wipeItems().yesImSure()
 val op2: Future[Unit] = collection.wipeIndices().yesImSure()
```


### Supported types

The mapping between MongoDB's BSON and scalas Map[String, Any] is determined by what BsonCodec (a Franklin type) you pick. 

```scala
trait BsonCodec {
  protected def mongoValue2Any(value: BSONValue): Any
  protected def any2MongoValue(value: Any): BSONValue

  def mongo2map(doc: BSONDocument): Map[String, Any] = {
    doc
      .elements
      .filter(_._1 != "_id")
      .toMap
      .mapValues(mongoValue2Any)
  }

  def map2mongo(map: Map[String, Any]): BSONDocument = {
    BSONDocument(map.mapValues(any2MongoValue))
  }
}
```

The default bson codec, aptly named DefaultBsonCodec - selected automatically if you do explicitly specify one, is defined as follows

```scala
case object DefaultBsonCodec extends BsonCodec {

  protected def mongoValue2Any(value: BSONValue): Any = {
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

  protected def any2MongoValue(value: Any): BSONValue = {
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

```
