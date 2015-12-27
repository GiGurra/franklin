# Franklin

.. is you personal scala librarian API. He stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing.  Franklin keeps a version number on each item stored in his collections - He is CAS capable and if a lot of people try to modify the same item at the *exact* same time, he will only let one of you do it. Every time an item is updated - its version number is automatically incremented.

Franklin is just a wrapper for a subset of ReactiveMongo with the option to run in-memory instead of against a mongodb database. Franklin gets a few new APIs every now and then. Franklin is just an asynchronous document/kv-storage using Futures. As every operation returns in a future, you can run them in parallell or sequence (e.g. in a for comprehension or async-await, whatever floats your boat) and/or fork/join at any point.

Franklin was initially created to support [valhalla-game](https://github.com/saiaku-gaming/valhalla-server) - and has been extended and tested as needed. Go ahead and use it for whatever purpose you want (MIT licensed) .. if you want to :). 

Also see [Franklin-Heisenberg](https://github.com/gigurra/franklin-heisenberg-bridge)

Franklin doesn't do any kind of authentication, so you better use him on a secure net or only talk to mongo on loopback!


## Examples

### What you need

In your build.sbt:
```sbt
.dependsOn(uri("git://github.com/GiGurra/franklin.git#0.1.4"))
```
In your code:
```scala
import se.gigurra.franklin.Franklin

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
