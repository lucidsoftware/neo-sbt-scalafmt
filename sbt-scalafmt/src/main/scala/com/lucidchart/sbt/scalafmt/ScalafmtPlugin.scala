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
                val directories = sourceDirectories.value
                scalafmtter.map(scalafmtter => (directories ** new ScalafmtFileFilter(scalafmtter)).get)
              } else {
                (baseDirectory, includeFilter, excludeFilter).map(
                  (base, include, exclude) => (base * include --- base * exclude).get
                )
              }
            }
            .value
        )
      )
    )

  override val requires = ScalafmtCorePlugin

  override val trigger = allRequirements

}
