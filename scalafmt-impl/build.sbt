disablePlugins(ScalafmtCorePlugin)

description := "Implementation for scalafmt-api"

def scalafmtOrg(scalafmtVersion: String): String = {
  if (scalafmtVersion.startsWith("2")) {
    "org.scalameta"
  } else {
    "com.geirsson"
  }
}

libraryDependencies += scalafmtOrg(SettingKey[String]("local-scalafmt-version").value) %% s"scalafmt-core" % SettingKey[String]("local-scalafmt-version").value % Provided
