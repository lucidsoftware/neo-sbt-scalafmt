package com.lucidchart.sbt.scalafmt

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import com.lucidchart.scalafmt.api.Dialect
import sbt.Keys._
import sbt._

object ScalafmtSbtPlugin extends AutoPlugin {

  object autoImport {
    val Sbt = config("sbt")
  }
  import autoImport._

  override val globalSettings = Seq(
    onLoad := { state =>
      onLoad.value {
        val projects = Def
          .settingDyn(Def.Initialize.join(loadedBuild.value.allProjectRefs.map(_._1).map { project =>
            (scalafmtOnCompile in (project, Sbt))(if (_) Some(project) else None)
          }))
          .value
          .flatten
        if (projects.isEmpty) {
          state
        } else {
          val command = CommandPlatform.CommandStrings.MultiTaskCommand +:
            projects.map(project => s"${Reference.display(project)}/$Sbt:${scalafmt.key}")
          command.mkString(" ") :: state
        }
      }
    }
  )

  override val projectSettings = inConfig(Sbt)(
    scalafmtCoreSettings ++
      Seq(
        scalafmtDialect := Dialect.SBT,
        sources in scalafmt := new ProjectDefinitionUtil(thisProject.value).sbtFiles.toSeq
      )
  )

  override val requires = ScalafmtCorePlugin

  override val trigger = allRequirements

}
