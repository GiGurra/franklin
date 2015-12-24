package se.gigurra.franklin

/**
  * Created by johan on 2015-12-23.
  */
object Franklin {

  def loadMongo(database: String = "local", nodes: Seq[String] = Seq("127.0.0.1:27017")): Store = ???
  def loadInMemory(): Store = ???

}
