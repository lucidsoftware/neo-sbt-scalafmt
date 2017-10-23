package com.lucidchart.sbt.scalafmt

import colordiff.ColorDiff
import com.google.common.cache._
import com.lucidchart.scalafmt.api.{Dialect, ScalafmtFactory, Scalafmtter}
import java.io.FileNotFoundException
import java.util.Arrays
import sbt.KeyRanks._
import sbt.Keys._
import sbt._
import sbt.plugins.{IvyPlugin, JvmPlugin}
import scala.collection.breakOut
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.control.Exception.catching

object ScalafmtCorePlugin extends AutoPlugin {

  object autoImport {
    case object UseScalafmtConfigFilter extends FileFilter {
      def accept(file: File) = ???
    }

    val Scalafmt = config("scalafmt").hide

    val ignoreErrors = TaskKey[Boolean]("ignore-errors", "Ignore errors for a task", BTask)

    val scalafmt = TaskKey[Unit]("scalafmt", "Format Scala sources", ATask)
    val scalafmtCache =
      SettingKey[Seq[File] => ScalafmtFactory]("scalafmtCache", "Cache of Scalafmtter instances", DSetting)
    val scalafmtCacheBuilder = SettingKey[CacheBuilder[Seq[File], ScalafmtFactory]](
      "scalafmt-cache-builder",
      "CacheBuilder for Scalafmtter cache",
      CSetting
    )
    val scalafmtConfig = TaskKey[File]("scalafmt-config", "Scalafmtter config file", BTask)
    val scalafmtDialect = SettingKey[Dialect]("scalafmt-dialect", "Dialect of Scala sources", BSetting)
    val scalafmtOnCompile = SettingKey[Boolean]("scalafmt-on-compile", "Format source when compiling", BTask)
    val scalafmtTestOnCompile =
      SettingKey[Boolean]("scalafmt-test-on-compile", "Check source format when compiling", BTask)
    // A better way is to put things in a sbt-scalafmt-ivy plugin, but this stuff is currently in flux.
    val scalafmtUseIvy = SettingKey[Boolean]("scalafmt-use-ivy", "Use sbt's Ivy resolution", CSetting)
    val scalafmtVersion = SettingKey[String]("scalafmt-version", "Scalafmtter version", AMinusSetting)
    val scalafmtter = TaskKey[Scalafmtter]("scalafmtter", "Scalafmt API")
    val scalafmtFailTest = SettingKey[Boolean](
      "scalafmt-Fail-Test",
      "Fail build when one or more style issues are found",
      CSetting
    )
    val scalafmtShowDiff = SettingKey[Boolean](
      "scalafmt-show-diff",
      "show differences between original and formatted version",
      CSetting
    )

    private[this] val scalafmtFn = Def.task {
      val ignoreErrors = this.ignoreErrors.value
      val logger = streams.value.log
      val scalafmtter = autoImport.scalafmtter.value.formatter(scalafmtDialect.value)
      (name: String, input: String) =>
        {
          try scalafmtter(input)
          catch {
            case NonFatal(e) =>
              val exceptionMesssage = e.getLocalizedMessage
              val message = if (exceptionMesssage.contains("<input>")) {
                exceptionMesssage.replace("<input>", name)
              } else {
                s"$name:\n$exceptionMesssage"
              }
              if (ignoreErrors) {
                logger.warn(message)
                input
              } else {
                throw new Exception(message, e)
              }
          }
        }
    }

    val scalafmtCoreSettings: Seq[Def.Setting[_]] =
      Seq(
        externalDependencyClasspath in scalafmt := (externalDependencyClasspath in Scalafmt).value,
        scalafmt := (scalafmt in scalafmt).value
      ) ++ inTask(scalafmt)(
        Seq(
          clean := IO.delete(streams.value.cacheDirectory),
          scalafmt := {
            val logger = streams.value.log
            val display = SbtUtil.display(thisProjectRef.value, configuration.value)

            val classpath = externalDependencyClasspath.value.map(_.data)
            val extraModified = (scalafmtConfig.value +: classpath).map(_.lastModified).max

            lazy val extraHash = Hash(
              classpath.toArray.flatMap(Hash(_)) ++
                catching(classOf[FileNotFoundException])
                  .opt(Hash(scalafmtConfig.value))
                  .getOrElse(Array.emptyByteArray)
            )

            // It would be simpler to use SBT's built-in FileFunction or similar, but this offers better performance and
            // doesn't take that much more work.
            // We first check timestamps, and only then check hashes.

            val cacheFile = streams.value.cacheDirectory / "scalafmt"
            val oldInfo: Map[File, HashModifiedFileInfo] = CachePlatform
              .readFileInfo(cacheFile)
              .map(info => info.file -> info)(breakOut)

            val updatedInfo = sources.value.map {
              source =>
                val old = oldInfo.getOrElse(source, CachePlatform.fileInfo(source, Nil, Long.MinValue))
                val updatedLastModified = extraModified max old.file.lastModified
                if (old.lastModified < updatedLastModified) {
                  val updatedHash = Hash(IO.readBytes(old.file) ++ extraHash)
                  val updatedInfo = CachePlatform.fileInfo(old.file, updatedHash.toList, updatedLastModified)
                  Either.cond(Arrays.equals(old.hash.toArray, updatedHash), updatedInfo, updatedInfo)
                } else {
                  Right(CachePlatform.fileInfo(old.file, old.hash, updatedLastModified))
                }
            }
            AnalysisPlatform.counted("Scala source", "", "s", updatedInfo.count(_.isLeft)).foreach { message =>
              logger.info(s"Formatting $message in $display ...")
            }

            val scalafmtter = scalafmtFn.value
            val newInfo = updatedInfo.map(_.left.flatMap { updatedInfo =>
              val input = IO.read(updatedInfo.file)
              val output = scalafmtter(updatedInfo.file.toString, input)
              Either.cond(
                input == output,
                updatedInfo, {
                  IO.write(updatedInfo.file, output)
                  CachePlatform.fileInfo(updatedInfo.file, Hash(output).toList, updatedInfo.file.lastModified)
                }
              )
            })
            AnalysisPlatform.counted("Scala source", "", "s", newInfo.count(_.isLeft)).foreach { message =>
              logger.info(s"Reformatted $message in $display")
            }

            CachePlatform.writeFileInfo(cacheFile, newInfo.map(_.merge)(breakOut))
          },
          scalafmtter := {
            val file = scalafmtConfig.value
            val logger = streams.value.log
            val configString = try IO.read(file)
            catch {
              case _: FileNotFoundException =>
                logger.debug(s"$file does not exist")
                ""
            }
            scalafmtCache.value(externalDependencyClasspath.value.map(_.data)).fromConfig(configString)
          },
          sources := Def.taskDyn {
            includeFilter.value match {
              case UseScalafmtConfigFilter =>
                val directories = sourceDirectories.value
                scalafmtter.map(scalafmtter => (directories ** new ScalafmtFileFilter(scalafmtter)).get)
              case _ => Defaults.collectFiles(sourceDirectories, includeFilter, excludeFilter)
            }
          }.value
        )
      ) ++ inTask(scalafmt)(
        Seq(
          test := {
            // Nothing is currently cached. This is probably okay since it is run for CI builds.
            val logger = streams.value.log
            val display = SbtUtil.display(thisProjectRef.value, configuration.value)
            AnalysisPlatform.counted("Scala source", "", "s", sources.value.size).foreach { message =>
              logger.info(s"Checking formatting for $message in $display ...")
            }

            val scalafmtter = scalafmtFn.value
            val failForStyleIssues = scalafmtFailTest.value
            val showDiff = scalafmtShowDiff.value
            val differentCount = sources.value.count {
              file =>
                val original = IO.read(file)
                val formatted = scalafmtter(file.toString, original)
                val hasChanges = original != formatted
                if (hasChanges) {
                  val msg = if (showDiff) {
                    val diff = ColorDiff(original.split('\n').toList, formatted.split('\n').toList)
                    s"$file has changes after scalafmt:\n$diff"
                  } else s"$file has changes after scalafmt"
                  if (failForStyleIssues) logger.error(msg)
                  else logger.warn(msg)
                }
                hasChanges
            }
            AnalysisPlatform.counted("Scala source", "", "s", differentCount).foreach { message =>
              val msg = s"$message not formatted in $display"
              if (failForStyleIssues) throw new ScalafmtCheckFailure(s"$message not formatted in $display")
              else logger.warn(msg)
            }
          }
        )
      )

    val scalafmtSettings = scalafmtCoreSettings ++
      Seq(
        compileInputs in compile := Def.taskDyn {
          val task =
            if (scalafmtOnCompile.value) scalafmt in resolvedScoped.value.scope
            else if (scalafmtTestOnCompile.value) test in (resolvedScoped.value.scope in scalafmt.key)
            else Def.task(())
          val previousInputs = (compileInputs in compile).value
          task.map(_ => previousInputs)
        }.value
      ) ++
      inTask(scalafmt)(
        Seq(
          scalafmtDialect := Dialect.SCALA,
          sourceDirectories := Seq(scalaSource.value)
        )
      )

  }
  import autoImport._

  override val buildSettings = Seq(
    ignoreErrors := true,
    includeFilter in scalafmt := UseScalafmtConfigFilter,
    scalafmtCache := {
      val cache = scalafmtCacheBuilder.value.build(new CacheLoader[Seq[File], ScalafmtFactory] {
        def load(classpath: Seq[File]) =
          new BridgeClassLoader(classpath.map(_.toURI.toURL))(!_.startsWith("com.lucidchart.scalafmt.api."))
            .loadClass("com.lucidchart.scalafmt.impl.ScalafmtFactory")
            .asInstanceOf[Class[_ <: ScalafmtFactory]]
            .newInstance
      })
      cache.get
    },
    scalafmtCacheBuilder := CacheBuilder.newBuilder
      .asInstanceOf[CacheBuilder[Seq[File], ScalafmtFactory]]
      .maximumSize(3),
    scalafmtConfig := (baseDirectory in ThisBuild).value / ".scalafmt.conf",
    scalafmtOnCompile := false,
    scalafmtTestOnCompile := false,
    scalafmtVersion := "0.6.8",
    scalafmtFailTest := true,
    scalafmtShowDiff := false
  )

  override val projectSettings = Seq(
    externalDependencyClasspath in Scalafmt := Classpaths.managedJars(Scalafmt, classpathTypes.value, update.value),
    ivyConfigurations ++= (if (scalafmtUseIvy.value) Seq(Scalafmt) else Seq.empty),
    libraryDependencies ++=
      (if (scalafmtUseIvy.value) (libraryDependencies in Scalafmt).value.map(_ % Scalafmt) else Seq.empty),
    libraryDependencies in Scalafmt := {
      val (scalaBinaryVersion, fmtVersion) = "(\\d+.){0,1}\\d+".r.findPrefixOf(scalafmtVersion.value) match {
        case Some("0.6")                         => ("2.11", "0.6")
        case Some("0.7" | "1.0" | "1.1" | "1.2") => ("2.12", "1.0")
        case _ =>
          println(s"Warning: Unknown Scalafmt version ${scalafmtVersion.value}; using 1.0 interface")
          ("2.12", "1.0")
      }
      val version = if (BuildInfo.version.endsWith("-SNAPSHOT")) {
        s"${BuildInfo.version.stripSuffix("-SNAPSHOT")}-$fmtVersion-SNAPSHOT"
      } else {
        s"${BuildInfo.version}-$fmtVersion"
      }
      Seq(
        "com.geirsson" % s"scalafmt-core_$scalaBinaryVersion" % scalafmtVersion.value,
        "com.lucidchart" % s"scalafmt-impl_$scalaBinaryVersion" % version
      )
    },
    scalafmtUseIvy := true
  ) :+ LibraryPlatform.moduleInfo(scalafmtUseIvy)

  override val requires = IvyPlugin && JvmPlugin

  override val trigger = allRequirements

}
