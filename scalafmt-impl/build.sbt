disablePlugins(ScalafmtCorePlugin)

description := "Implementation for scalafmt-api"

scalaVersion := "2.12.2"

libraryDependencies += "com.geirsson" %% s"scalafmt-core" % "1.2.0" % Provided
