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
val collection: Collection = provider.getOrCreate("test_objects")
// If you want to add some more mongodb magic you can cast this to a 
// *case class MongoCollection(collection: BSONCollection)*
// and access the underlying reactivemongo collection directly.
```

### Create some indices

```scala
val op1: Future[Unit] = store.createIndex("guid", unique = true)
val op2: Future[Unit] = store.createIndex("items", unique = false)
```

### Store some data

```scala

store.createIndex("id", unique = true).await()

val a = Map("id" -> "a", "somedata" -> 1)
val b = Map("id" -> "b", "somedata" -> 1)

val aOp: Future[Unit] = store.create(a)
val bOp: Future[Unit] = store.create(b)

// Trying to create the same object again
// with a unique index conflict will eventually
// complete the returned future with an 
// ItemAlreadyExists exception.
val bOp2: Future[Unit] = store.create(b)

```

### Find some data

```scala

// Either access the data with raw 'select-like' statements/filters
val data1: Future[Seq[Item]] = collection.where("id" -> "a", "somedata" -> 1).find

// Or pass in a map
val query = Map("id" -> "a")
val data2: Future[Seq[Item]] = collection.where(query).find

// An Item is:
// case class Item(data: Map[String, Any], version: Long)

```

### Update some data

Per document completely replace the previous content. They are also atomic - per mongodb design (as is the franklin in-memory implementation).

```scala

// You specify the item to replace, its new data, if upsert (create new if missing), and the expected version.
// The expectVersion is the version you expect the previously stored data to have. If you specify the wrong version,
// The returned future will eventually complete with an exception (probably a WrongDataVersion exception)
// The starting version number when they're first created in the database is 1
val op1: Future[Unit] = store.where("id" -> "a").update(Map("id" -> "a", "ouf" -> 3321), upsert = false, expectVersion = 3)

// To ignore the data version & invite race conditions - omit the expectVersion parameter or set it to -1
val op2: Future[Unit] = store.where("id" -> "a").update(Map("id" -> "b", "ouf" -> 123))

```

### Append some data

```scala

```

### Wipe everything

```scala
 val op1: Future[Unit] = store.wipeItems().yesImSure()
 val op2: Future[Unit] = store.wipeIndices().yesImSure()
```
