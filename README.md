# neo-sbt-scalafmt

[![Build Status](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt.svg)](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt)
[![Maven Version](https://img.shields.io/maven-central/v/com.lucidchart/scalafmt-api.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.lucidchart%22%20AND%20a%3A%22scalafmt-api%22)

An sbt plugin for [Scalafmt](http://scalameta.org/scalafmt/) that

* formats .sbt and .scala files
* supports sbt 0.13 and 1.0.0-RC3
* supports Scalafmt 0.6 and 1.0
* runs in-process
* uses sbt's ivy2 for dependency resolution

## Usage

In `project/plugins.sbt`,

```scala
// see the Maven badge at the top of this README for the latest version

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "<version>")
// if you use coursier, you must use sbt-scalafmt-coursier
// addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "<version>")
```

then

```
> scalafmt       # format compile sources
> test:scalafmt  # format test sources
> sbt:scalafmt   # format .sbt source
```

To ensure everything is formatted, and fail if it is not (e.g. as a CI step),

```
> scalafmt::test      # check compile sources
> test:scalafmt::test # check test sources
> sbt:scalafmt::test  # check .sbt sources
```

## Scalafmt configuration

By default, `.scalafmt.conf` in the root project is used for Scalafmt configuration. If the file does not exist, the
Scalafmt defaults are used. To choose another config file,

```scala
scalafmtConfig in ThisBuild := file("other.scalafmt.conf") // all projects
scalafmtConfig := file("other.scalafmt.conf")              // current project
scalafmtConfig in Compile := file("other.scalafmt.conf")   // current project, specific configuration
```

To change the Scalafmt version,

```scala
scalafmtVersion in ThisBuild := "1.0.0-RC2" // all projects
scalafmtVersion := "1.0.0-RC2"              // current project
```

## Task configuration

To run `scalafmt` automatically before compiling (or before loading, in the case of sbt).

```scala
scalafmtOnCompile in ThisBuild := true // all projects
scalafmtOnCompile := true              // current project
scalafmtOnCompile in Compile := true   // current project, specific configuration
```

To run `scalafmt::test` automatically before compiling (or before loading, in the case of sbt).

```scala
scalafmtTestOnCompile in ThisBuild := true // all projects
scalafmtTestOnCompile := true              // current project
scalafmtTestOnCompile in Compile := true   // current project, specific configuration
```

By default, `scalafmt::test` fails if sources are unformatted. If you'd prefer warnings instead: 

```scala
scalafmtFailTest in ThisBuild := false // all projects
scalafmtFailTest := false              // current project
scalafmtFailTest in Compile := false   // current project, specific configuration
```

At the time of writing, Scalafmt fails on some valid inputs. By default, errors in Scalafmt itself do not fail the
`scalafmt` task. To fail instead,

```scala
ignoreErrors in (ThisBuild, scalafmt) := false // all projects
ignoreErrors in scalafmt := false              // current project
ignoreErrors in (Compile, scalafmt) := false   // current project, specific configuration
```

By default, scalafmt just lists the files that have differences. You can configure it to show the actual diff like this:

```scala
scalafmtShowDiff in (ThisBuild, scalafmt) := false // all projects
scalafmtShowDiff in scalafmt := false              // current project
scalafmtShowDiff in (Compile, scalafmt) := false   // current project, specific configuration
```

## Additional configuration

The scalafmt task is defined by default for the compile and test configurations. To define it for additional
configurations, e.g. `Integration`,

```scala
inConfig(Integration)(scalafmtSettings)
```

To disable this plugin for a project

```scala
disablePlugins(ScalafmtCorePlugin)
```

## Formatting build files

If you wish to format project/*.scala files, configure the meta-build by adding sbt-scalafmt to
project/project/plugins.sbt, and configuring it in project/plugins.sbt. See
[sbt documentation](http://www.scala-sbt.org/0.13/docs/Organizing-Build.html) on meta-builds.

## Implementation details

Scalafmt artifacts are downloaded with a scalafmt Ivy configuration added to each project. Scalafmt classes are loaded
in a separate classloader, allowing them work regardless of the Scala version of sbt.

* `ScalafmtCorePlugin` adds the Ivy configuration and scalafmt dependency.
* `ScalafmtCoursierPlugin` replaces the sbt ivy configuration with coursier.
* `ScalafmtSbtPlugin` create scalafmt tasks for .sbt sources.
* `ScalafmtPlugin` creates the scalafmt task for compile and test configurations.
