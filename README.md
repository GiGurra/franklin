# Franklin

.. is you personal scala librarian API. He stores and finds the stuff you need, either to/from MongoDB for production purposes, or in-memory for testing. 

Franklin is just a wrapper for a subset of ReactiveMongo with the option to run in-memory instead of against a mongodb database. Franklin gets a few new APIs every now and then. Franklin APIs should never really expose any mongodb details. Franklin is just an asynchronous document/kv-storage using Futures.

Extended and tested as needed. Not really meant for anyone else to use, but go ahead (MIT licensed) if you want to. Also see [Franklin-Heisenberg](https://github.com/gigurra/franklin-heisenberg-bridge)

Franklin was created to support [valhalla-game](https://github.com/saiaku-gaming/valhalla-server).


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
```

### Create some indices

```scala

```

