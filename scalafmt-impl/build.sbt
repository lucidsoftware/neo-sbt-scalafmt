disablePlugins(ScalafmtCorePlugin)

description := "Implementation for scalafmt-api"

libraryDependencies += "com.geirsson" %% s"scalafmt-core" % SettingKey[String]("local-scalafmt-version").value % Provided
