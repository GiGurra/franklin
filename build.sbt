name := "franklin"

organization := "se.gigurra"

version := getVersion

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.scalatest"     %%  "scalatest"       %   "2.2.4"     %   "test",
  "org.mockito"       %   "mockito-core"    %   "1.10.19"   %   "test",
	"org.reactivemongo" %% "reactivemongo"    %   "0.11.9"
)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"


def getVersion: String = {

	val v = scala.util.Properties.envOrNone("FRANKLIN_VERSION").getOrElse{
		println(s"No 'FRANKLIN_VERSION' defined - defaulting to SNAPSHOT")
		"SNAPSHOT"
	}
	
	println(s"Building Franklin v. $v")
	v
}

