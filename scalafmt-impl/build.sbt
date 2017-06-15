disablePlugins(ScalafmtCorePlugin)

autoScalaLibrary := false

crossPaths := false

libraryDependencies += "com.geirsson" %% s"scalafmt-core" % SettingKey[String]("local-scalafmt-version").value % Provided
