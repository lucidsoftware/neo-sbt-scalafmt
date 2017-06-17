package com.lucidchart.scalafmt.coursier

import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import coursier._
import java.io.{FileNotFoundException, IOException}
import sbt.KeyRanks._
import sbt.Keys._
import sbt._
import scala.util.control.NonFatal
import scalaz.concurrent.Task

/**
 * This does not pull from sbt settings like sbt-coursier. It is a quickish fix to some issues like
 * https://github.com/lucidsoftware/neo-sbt-scalafmt/issues/1 .
 */
object ScalafmtCoursierPlugin extends AutoPlugin {

  object autoImport {
    val scalafmtCoursierRepositories =
      SettingKey[Seq[Repository]]("scalafmt-coursier-repositories", "Coursier respositories for scalafmt", BTask)
  }
  import autoImport._

  override val projectSettings = Seq(
    externalDependencyClasspath in Scalafmt := {
      val dependencies = (libraryDependencies in Scalafmt).value
        .map(module => Dependency(Module(module.organization, module.name), module.revision))
        .toSet
      val cacheFile = streams.value.cacheDirectory / "dependencies"
      val newHash = dependencies.hashCode
      val cached = try {
        IO.readLines(cacheFile) match {
          case hash +: fileStrings =>
            val files = fileStrings.map(file)
            if (hash.toInt == newHash && files.forall(_.exists)) Some(files) else None
        }
      } catch {
        case _: FileNotFoundException => None
        case NonFatal(e) =>
          streams.value.log.error(e.getLocalizedMessage)
          None
      }
      Attributed.blankSeq(cached.getOrElse {
        synchronized {
          val fetch = Fetch.from(scalafmtCoursierRepositories.value, coursier.Cache.fetch())
          streams.value.log.info(s"Fetching scalafmt for ${Reference.display(thisProjectRef.value)}")
          val resolution = Resolution(dependencies).process.run(fetch).unsafePerformSync
          val result = Task
            .gatherUnordered(resolution.artifacts.map(coursier.Cache.file(_).run))
            .unsafePerformSync
            .map(_.valueOr(error => throw new IOException(error.describe)))
            .filter(_.ext == "jar")
          streams.value.log.info(s"Fetched ${result.size} artifacts for scalafmt")
          IO.writeLines(cacheFile, newHash.toString +: result.map(_.toString))
          result
        }
      })
    },
    scalafmtUseIvy := false
  )

  override val buildSettings = Seq(
    scalafmtCoursierRepositories := Seq(
      coursier.Cache.ivy2Local,
      coursier.MavenRepository("https://repo1.maven.org/maven2")
    )
  )

  override val requires = ScalafmtCorePlugin

  override val trigger = allRequirements

}
