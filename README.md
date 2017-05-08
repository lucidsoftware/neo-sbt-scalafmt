# neo-sbt-scalafmt

[![Build Status](https://travis-ci.org/lucidsoftware/relate.svg)](https://travis-ci.org/lucidsoftware/relate)
[![Maven Version](https://img.shields.io/maven-central/v/com.lucidchart/scalafmt-api.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.lucidchart%22%20AND%20a%3A%22scalafmt-api%22)

A SBT plugin for Scalafmt that

* supports SBT 0.13 and 1.0.0-M5
* supports Scalafmt 0.6 and 0.7
* runs in-process
* uses SBT's ivy2 for dependency resolution

## Usage

In `project/plugins.sbt`,

```scala
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "<version>")
// see the Maven badge above for the latest version
```

Then enable it in your projects

```scala
enablePlugins(ScalafmtPlugin)
```

Then

```
> scalafmt       # format compile sources
> test:scalafmt  # format test sources
```

If you want to ensure everything is formatted (e.g. as a CI step),

```
> scalafmt::test      # check compile sources
> test:scalafmt::test # check test sources
```

## Additional configuration

By default, `.scalafmt.conf` is used for Scalafmt configuration. To choose another location

```scala
scalafmtConfig := (baseDirectory in ThisBuild).value / "other.scalafmt.conf"
// can be set per-project, per-configuration
```

To change the Scalafmt version,

```scala
scalafmtVersion := "0.7.0-RC1"
// can be set per-project
```

By default, Scalafmt runs before compiling. You can change that with

```scala
scalafmtOnCompile := false
// can be set per-project, per-configuration
```

`ScalafmtCorePlugin` defines most of the settings. `ScalaPlugin` applies them to the compile and test configurations.
To apply them to additional configurations

```scala
inConfig(Integration)(scalafmtSettings)
```

## Implementation details

Loading Scalafmt in a separate classloader allows sbt-scalafmt to work across sbt and Scalafmt versions, regardless of
the Scala versions used.

Scalafmt artifacts are downloaded with a scalafmt Ivy configuration. The configuration is add to each project, so if you
have a lot of projects, you may see `update` times increase a little. Fortunately, this is re-run infrequently.
