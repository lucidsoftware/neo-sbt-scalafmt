sys.props.get("plugin.version") match {
  case Some(v) => addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % v)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
