// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.github.gigurra"

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url>https://github.com/GiGurra/franklin</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>https://github.com/GiGurra/franklin/blob/master/LICENSE</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/gigurra/franklin</connection>
    <developerConnection>scm:git:git@github.com:gigurra/franklin</developerConnection>
    <url>github.com/gigurra/franklin</url>
  </scm>
  <developers>
    <developer>
      <id>gigurra</id>
      <name>Johan Kjölhede</name>
      <url>https://github.com/GiGurra/franklin</url>
    </developer>
  </developers>
}

