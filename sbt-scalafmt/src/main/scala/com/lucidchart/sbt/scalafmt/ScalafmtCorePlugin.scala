package com.lucidchart.sbt.scalafmt

import com.google.common.cache._
import com.lucidchart.scalafmt.api.Scalafmtter
import java.io.FileNotFoundException
import java.util.Arrays
import sbt.KeyRanks._
import sbt.Keys._
import sbt._
import sbt.plugins.{IvyPlugin, JvmPlugin}
import scala.collection.breakOut
import scala.util.control.NonFatal

object ScalafmtCorePlugin extends AutoPlugin {

  object autoImport {
    val Scalafmt = config("scalafmt").hide

    val ignoreErrors = TaskKey[Boolean]("ignore-errors", "Ignore errors for a task", BTask)

    val scalafmt = TaskKey[Unit]("scalafmt", "Format Scala sources", ATask)
    val scalafmtBridge = SettingKey[ModuleID]("scalafmt-bridge", "scalafmt-impl dependency", BMinusSetting)
    val scalafmtCache = SettingKey[Seq[File] => Scalafmtter]("scalafmtCache", "Cache of Scalafmt instances", DSetting)
    val scalafmtCacheBuilder = SettingKey[CacheBuilder[Seq[File], Class[_ <: Scalafmtter]]](
      "scalafmt-cache-builder",
      "CacheBuilder for Scalafmt cache",
      CSetting
    )
    val scalafmtConfig = TaskKey[File]("scalafmt-config", "Scalafmt config file", BTask)
    val scalafmtOnCompile = TaskKey[Boolean]("scalafmt-on-compile", "Format source when compiling", BTask)
    val scalafmtVersion = SettingKey[String]("scalafmt-version", "Scalafmt version", AMinusSetting)

    private[this] val scalafmtConfigString = Def.task {
      val file = scalafmtConfig.value
      val logger = streams.value.log
      () =>
        try IO.read(file)
        catch {
          case _: FileNotFoundException =>
            logger.debug(s"$file does not exist")
            ""
        }
    }

    private[this] val scalafmtter = Def.task {
      val classpath = externalDependencyClasspath.value
      val cache = scalafmtCache.value
      val ignoreErrors = this.ignoreErrors.value
      val logger = streams.value.log
      (configString: String) =>
        {
          val scalafmtter = cache(classpath.map(_.data))
          val config = scalafmtter.config(configString)
          (name: String, input: String) =>
            {
              try scalafmtter.format(config, input)
              catch {
                case NonFatal(e) if ignoreErrors =>
                  val exceptionMesssage = e.getLocalizedMessage
                  val message = if (exceptionMesssage.contains("<input>")) {
                    exceptionMesssage.replace("<input>", name)
                  } else {
                    s"$name:\n$exceptionMesssage"
                  }
                  logger.warn(message)
                  input
              }
            }
        }
    }

    val scalafmtSettings: Seq[Def.Setting[_]] =
      Seq(
        compileInputs in compile := Def.taskDyn {
          val task = if (scalafmtOnCompile.value) scalafmt in resolvedScoped.value.scope else Def.task(())
          val previousInputs = (compileInputs in compile).value
          task.map(_ => previousInputs)
        }.value,
        scalafmt := (scalafmt in scalafmt).value
      ) ++ inTask(scalafmt)(
        Seq(
          clean := IO.delete(streams.value.cacheDirectory),
          externalDependencyClasspath := Classpaths.managedJars(Scalafmt, classpathTypes.value, update.value),
          sourceDirectories := Seq(scalaSource.value),
          scalafmt := {
            val logger = streams.value.log
            val display = SbtUtil.display(thisProjectRef.value, configuration.value)

            val classpath = externalDependencyClasspath.value.map(_.data)
            val extraModified = (scalafmtConfig.value +: classpath).map(_.lastModified).max

            lazy val configString = scalafmtConfigString.value()
            lazy val extraHash = Hash(classpath.toArray.flatMap(Hash(_)) ++ Hash(configString))
            lazy val scalafmtter = this.scalafmtter.value(configString)

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
          sources := Defaults.collectFiles(sourceDirectories, includeFilter, excludeFilter).value
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

            val scalafmtter = this.scalafmtter.value(scalafmtConfigString.value())
            val differentCount = sources.value.count { file =>
              val content = IO.read(file)
              val hasChanges = content != scalafmtter(file.toString, content)
              if (hasChanges) {
                logger.error(s"$file has changes after scalafmt")
              }
              hasChanges
            }
            AnalysisPlatform.counted("Scala source", "", "s", differentCount).foreach { message =>
              throw new ScalafmtCheckFailure(s"$message not formatted in $display")
            }
          }
        )
      )
  }
  import autoImport._

  override val buildSettings = Seq(
    ignoreErrors := true,
    scalafmtCache := {
      val cache = scalafmtCacheBuilder.value.build(new CacheLoader[Seq[File], Class[_ <: Scalafmtter]] {
        def load(classpath: Seq[File]) = {
          val classLoader =
            new BridgeClassLoader(classpath.map(_.toURI.toURL))(!_.startsWith("com.lucidchart.scalafmt.api."))
          classLoader.loadClass("com.lucidchart.scalafmt.impl.Scalafmtter").asInstanceOf[Class[_ <: Scalafmtter]]
        }
      })
      cache.get(_).newInstance
    },
    scalafmtCacheBuilder := CacheBuilder.newBuilder
      .asInstanceOf[CacheBuilder[Seq[File], Class[_ <: Scalafmtter]]]
      .maximumSize(3),
    scalafmtConfig := (baseDirectory in ThisBuild).value / ".scalafmt.conf",
    scalafmtOnCompile := false,
    scalafmtVersion := "0.6.8"
  )

  override val projectSettings = Seq(
    includeFilter in scalafmt := "*.scala",
    ivyConfigurations += Scalafmt,
    ivyScala := ivyScala.value
      .map(LibraryPlatform.withOverrideScalaVersion(_, false)), // otherwise scala-library conflicts
    libraryDependencies ++= Seq(
      "com.geirsson" % "scalafmt-core_2.11" % scalafmtVersion.value % Scalafmt,
      scalafmtBridge.value % Scalafmt
    ),
    scalafmtBridge := {
      val sVersion = scalafmtVersion.value.split("\\.", -1).toSeq match {
        case "0" +: "6" +: _ => "0.6"
        case "0" +: "7" +: _ => "0.7"
        case "1" +: "0" +: _ => "0.7"
        case _ =>
          println(s"Warning: Unknown Scalafmt version ${scalafmtVersion.value}")
          "0.7"
      }
      val version = if (BuildInfo.version.endsWith("-SNAPSHOT")) {
        s"${BuildInfo.version.stripSuffix("-SNAPSHOT")}-$sVersion-SNAPSHOT"
      } else {
        s"${BuildInfo.version}-$sVersion"
      }
      "com.lucidchart" % s"scalafmt-impl" % version
    }
  )

  override val requires = IvyPlugin && JvmPlugin

  override val trigger = allRequirements

}
