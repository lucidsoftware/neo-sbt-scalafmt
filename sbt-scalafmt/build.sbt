enablePlugins(BuildInfoPlugin, ScalafmtPlugin)

buildInfoKeys := Seq[BuildInfoKey.Entry[_]](version)

buildInfoPackage := "com.lucidchart.sbt.scalafmt"

description := "SBT plugin for scalafmt"

libraryDependencies ++= Seq(
  "com.google.code.findbugs" % "jsr305" % "3.0.2" % Provided, // fixes warnings about javax annotations
  "com.google.guava" % "guava" % "19.0",
  "com.michaelpollmeier" %% "colordiff" % "0.8"
)

sbtPlugin := true

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)
