package com.lucidchart.sbt.scalafmt

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt._

object ScalafmtPlugin extends AutoPlugin {

  override val projectSettings = Seq(Compile, Test).flatMap(inConfig(_)(scalafmtSettings))

  override val requires = ScalafmtCorePlugin

}
