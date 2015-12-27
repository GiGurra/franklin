# Franklin

.. is you personal scala librarian API. He stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing.  Franklin keeps a version number on each item stored in his collections - He is CAS capable and if a lot of people try to modify the same item at the *exact* same time, he will only let one of you do it. Every time an item is updated - its version number is automatically incremented.

Franklin is just a wrapper for a subset of ReactiveMongo with the option to run in-memory instead of against a mongodb database. Franklin gets a few new APIs every now and then. Franklin APIs should never really expose any mongodb details. Franklin is just an asynchronous document/kv-storage using Futures.

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
// If you're sure to use mongodb or want to extend Franklin 
// functionality, you can cast this to a 
// *case class MongoCollection(collection: BSONCollection)*
// and access the underlying collection directly.
```

### Create some indices

```scala
val op1: Future[Unit] = store.createIndex("guid", unique = true)
val op2: Future[Unit] = store.createIndex("items", unique = false)
```

### Store some data

```scala

```

### Find some data

```scala

```

### Update some data

```scala

```

### Update some data

```scala

```

### Wipe everything

```scala
 val op1: Future[Unit] = store.wipeItems().yesImSure()
 val op2: Future[Unit] = store.wipeIndices().yesImSure()
```
