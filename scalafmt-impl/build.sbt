autoScalaLibrary := false

crossPaths := false

libraryDependencies += "com.geirsson" %% s"scalafmt-core" % SettingKey[String]("scalafmt-version").value % Provided
