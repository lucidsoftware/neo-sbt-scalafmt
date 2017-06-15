lazy val root = (project in file("."))
  .enablePlugins(ScalafmtPlugin)
  .settings(scalafmtOnCompile := true)
