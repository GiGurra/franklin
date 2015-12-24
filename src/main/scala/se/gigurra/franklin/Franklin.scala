package se.gigurra.franklin

import se.gigurra.franklin.inmemimpl.ImMemStore
import se.gigurra.franklin.mongoimpl.MongoDb

/**
  * Created by johan on 2015-12-23.
  */
object Franklin {

  def loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017")): Store =
    MongoDb.connect(nodes).getDb(database)

  def loadInMemory(): Store = ImMemStore()

}
