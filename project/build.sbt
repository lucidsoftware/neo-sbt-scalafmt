libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")

addSbtPlugin("com.lucidchart" % "sbt-cross" % "3.2")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "0.2")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
