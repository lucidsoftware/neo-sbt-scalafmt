package com.lucidchart.sbt.scalafmt

import com.google.common.cache._
import com.lucidchart.scalafmt.api.{Dialect, ScalafmtFactory}
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
    val scalafmtCache =
      SettingKey[Seq[File] => ScalafmtFactory]("scalafmtCache", "Cache of Scalafmt instances", DSetting)
    val scalafmtCacheBuilder = SettingKey[CacheBuilder[Seq[File], ScalafmtFactory]](
      "scalafmt-cache-builder",
      "CacheBuilder for Scalafmt cache",
      CSetting
    )
    val scalafmtConfig = TaskKey[File]("scalafmt-config", "Scalafmt config file", BTask)
    val scalafmtDialect = SettingKey[Dialect]("scalafmt-dialect", "Dialect of Scala sources", BSetting)
    val scalafmtOnCompile = SettingKey[Boolean]("scalafmt-on-compile", "Format source when compiling", BTask)
    // A better way is to put things in a sbt-scalafmt-ivy plugin, but this stuff is currently in flux.
    val scalafmtUseIvy = SettingKey[Boolean]("scalafmt-use-ivy", "Use sbt's Ivy resolution", CSetting)
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
      val dialect = scalafmtDialect.value
      val ignoreErrors = this.ignoreErrors.value
      val logger = streams.value.log
      (configString: String) =>
        {
          val scalafmtter = cache(classpath.map(_.data)).fromConfig(configString)(dialect)
          (name: String, input: String) =>
            {
              try scalafmtter.format(input)
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

    val scalafmtSettings = scalafmtCoreSettings ++
      Seq(
        compileInputs in compile := Def.taskDyn {
          val task = if (scalafmtOnCompile.value) scalafmt in resolvedScoped.value.scope else Def.task(())
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
    scalafmtVersion := "0.6.8"
  )

  override val projectSettings = Seq(
    externalDependencyClasspath in Scalafmt := Classpaths.managedJars(Scalafmt, classpathTypes.value, update.value),
    includeFilter in scalafmt := "*.scala",
    ivyConfigurations ++= (if (scalafmtUseIvy.value) Seq(Scalafmt) else Seq.empty),
    ivyScala := {
      if (scalafmtUseIvy.value) {
        // otherwise scala-library conflicts
        ivyScala.value.map(LibraryPlatform.withOverrideScalaVersion(_, false))
      } else {
        ivyScala.value
      }
    },
    libraryDependencies ++= {
      if (scalafmtUseIvy.value) {
        Seq(
          "com.geirsson" % "scalafmt-core_2.11" % scalafmtVersion.value % Scalafmt,
          scalafmtBridge.value % Scalafmt
        )
      } else {
        Seq.empty
      }
    },
    scalafmtBridge := {
      val (scalaBinaryVersion, fmtVersion) = "(\\d+.){0,1}\\d+".r.findPrefixOf(scalafmtVersion.value) match {
        case Some("0.6")         => ("2.11", "0.6")
        case Some("0.7" | "1.0") => ("2.12", "1.0")
        case _ =>
          println(s"Warning: Unknown Scalafmt version ${scalafmtVersion.value}; using 1.0 interface")
          "1.0"
      }
      val version = if (BuildInfo.version.endsWith("-SNAPSHOT")) {
        s"${BuildInfo.version.stripSuffix("-SNAPSHOT")}-$fmtVersion-SNAPSHOT"
      } else {
        s"${BuildInfo.version}-$fmtVersion"
      }
      "com.lucidchart" % s"scalafmt-impl_$scalaBinaryVersion" % version
    },
    scalafmtUseIvy := true
  )

  override val requires = IvyPlugin && JvmPlugin

  override val trigger = allRequirements

}
