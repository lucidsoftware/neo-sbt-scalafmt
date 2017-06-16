disablePlugins(ScalafmtCorePlugin)

libraryDependencies += "com.geirsson" %% s"scalafmt-core" % SettingKey[String]("local-scalafmt-version").value % Provided

scalaVersion := "2.11.8"
