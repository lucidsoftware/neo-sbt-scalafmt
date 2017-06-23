# neo-sbt-scalafmt

[![Build Status](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt.svg)](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt)
[![Maven Version](https://img.shields.io/maven-central/v/com.lucidchart/scalafmt-api.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.lucidchart%22%20AND%20a%3A%22scalafmt-api%22)

An sbt plugin for [Scalafmt](http://scalameta.org/scalafmt/) that

* formats .sbt and .scala files
* supports SBT 0.13 and 1.0.0-M6
* supports Scalafmt 0.6 and 1.0
* runs in-process
* uses sbt's ivy2 for dependency resolution

## Usage

In `project/plugins.sbt`,

```scala
// see the Maven badge at the top of this README for the latest version

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "<version>")
// if you use coursier, you must use sbt-scalafmt-coursier to avoid a bug
// addSbtPlugin("com.lucidchart" % "sbt-scalafmt-coursier" % "<version>")
```

then

```
> scalafmt       # format compile sources
> test:scalafmt  # format test sources
> sbt:scalafmt   # format .sbt source
```

If you want to ensure everything is formatted, and fail if it is not (e.g. as a CI step),

```
> scalafmt::test      # check compile sources
> test:scalafmt::test # check test sources
> sbt:scalafmt::test  # check .sbt sources
```

As of 1.7, source file filters come from the Scalafmt configuration file.

## Additional configuration

By default, `.scalafmt.conf` is used for Scalafmt configuration. To choose another location

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

To run scalafmt automatically before compiling (or before loading, in the case of sbt).

```scala
scalafmtOnCompile in ThisBuild := true // all projects
scalafmtOnCompile := true              // current project
scalafmtOnCompile in Compile := true   // current project, specific configuration
```

By default, scalafmt fails the build for any style issues. If you'd prefer warnings instead: 

```scala
scalafmtFailTest in ThisBuild := false // all projects
scalafmtFailTest := false              // current project
scalafmtFailTest in Compile := false   // current project, specific configuration
```

Most scalafmt errors do not fail the scalafmt task. To fail the task for any scalafmt errors.

```scala
ignoreErrors in (ThisBuild, scalafmt) := false // all projects
ignoreErrors in scalafmt := false              // current project
ignoreErrors in (Compile, scalafmt) := false   // current project, specific configuration
```

The scalafmt task is defined by default for the compile and test configurations. To define it for additional
configurations, e.g. `Integration`,

```scala
inConfig(Integration)(scalafmtSettings)
```

To disable this plugin for a project

```scala
disablePlugins(ScalafmtCorePlugin)
```

## Formatting project/*.scala

If you wish to format Scala build files in project/, configure the meta-build, by adding sbt-scalafmt to
project/project/plugins.sbt, and configuring it in project/plugins.sbt.

## Implementation details

Scalafmt artifacts are downloaded with a scalafmt Ivy configuration added to each project. Scalafmt classes are loaded
in a separate classloader, allowing them work regardless of the Scala version of sbt.

* `ScalafmtCorePlugin` adds the Ivy configuration and scalafmt dependency.
* `ScalafmtCoursierPlugin` replaces the sbt ivy configuration with coursier.
* `ScalafmtSbtPlugin` create scalafmt tasks for .sbt sources.
* `ScalafmtPlugin` creates the scalafmt task for compile and test configurations.
