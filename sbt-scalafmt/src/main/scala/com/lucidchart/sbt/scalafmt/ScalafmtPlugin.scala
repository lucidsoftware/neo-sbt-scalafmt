package com.lucidchart.sbt.scalafmt

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt._
import sbt.Keys._

object ScalafmtPlugin extends AutoPlugin {

  override val projectSettings = Seq(Compile, Test).flatMap(inConfig(_)(scalafmtSettings)) ++
    inConfig(Compile)(
      inTask(scalafmt)(
        Seq(
          sources ++= {
            val finder = (baseDirectory.value * includeFilter.value) --- (baseDirectory.value * excludeFilter.value)
            if (sourcesInBase.value) finder.get else Seq.empty
          }
        )
      )
    )

  override val requires = ScalafmtCorePlugin

  override val trigger = allRequirements

}
