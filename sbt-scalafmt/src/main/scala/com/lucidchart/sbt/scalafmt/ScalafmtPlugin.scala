package com.lucidchart.sbt.scalafmt

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt._
import sbt.Keys._

object ScalafmtPlugin extends AutoPlugin {

  override val projectSettings = Seq(Compile, Test).flatMap(inConfig(_)(scalafmtSettings)) ++
    inConfig(Compile)(
      inTask(scalafmt)(
        Seq(
          sources ++= Def
            .taskDyn[Seq[File]] {
              if (!sourcesInBase.value) {
                Def.task(Seq.empty[File])
              } else if (includeFilter.value == UseScalafmtConfigFilter) {
                Def.task((baseDirectory.value * (new ScalafmtFileFilter(scalafmtter.value ) && "*.scala")).get)
              } else {
                Def.task((baseDirectory.value * includeFilter.value --- baseDirectory.value * excludeFilter.value).get)
              }
            }
            .value
        )
      )
    )

  override val requires = ScalafmtCorePlugin

  override val trigger = allRequirements

}
