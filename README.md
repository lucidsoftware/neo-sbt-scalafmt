# neo-sbt-scalafmt

[![Build Status](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt.svg)](https://travis-ci.org/lucidsoftware/neo-sbt-scalafmt)
[![Maven Version](https://img.shields.io/maven-central/v/com.lucidchart/scalafmt-api.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.lucidchart%22%20AND%20a%3A%22scalafmt-api%22)

A SBT plugin for Scalafmt that

* supports SBT 0.13 and 1.0.0-M6
* supports Scalafmt 0.6, 0.7, and 1.0
* runs in-process
* uses SBT's ivy2 for dependency resolution

## Usage

In `project/plugins.sbt`,

```scala
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "<version>")
// see the Maven badge at the top of this README for the latest version
```

Then

```
> scalafmt       # format compile sources
> test:scalafmt  # format test sources
```

If you want to ensure everything is formatted, and fail if it is not (e.g. as a CI step),

```
> scalafmt::test      # check compile sources
> test:scalafmt::test # check test sources
```

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

As of 1.0, ScalafmtPlugin is enabled automatically, but does not run scalafmt automatically. To run scalafmt
automatically before compiling.

```scala
scalafmtOnCompile in ThisBuild := true // all projects
scalafmtOnCompile := true              // current project
scalafmtOnCompile in Compile := true   // current project, specific configuration
```

Most scalafmt errors do not fail the scalafmt task. To fail the task for any scalafmt errors.

```scala
ignoreErrors in (ThisBuild, scalafmt) := false // all projects
ignoreErrors in scalafmt := false              // current project
ignoreErrors in (Compile, scalafmt) := false   // current project, specific configuration
```

`ScalafmtCorePlugin` defines most of the settings. `ScalaPlugin` applies them to the compile and test configurations.
To apply them to additional configurations

```scala
inConfig(Integration)(scalafmtSettings)
```

## Implementation details

Scalafmt artifacts are downloaded with a scalafmt Ivy configuration added to each project. Scalafmt classes are loaded
in a separate classloader, allowing them work regardless of the Scala version of SBT.
