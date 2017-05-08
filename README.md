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
> scalafmt
```

## Additional configuration

By default, `.scalafmt.conf` is used for Scalafmt configuration. To choose another location

```scala
scalafmtConfig := (baseDirectory in ThisBuild).value / "other.scalafmt.conf"
// can be set per-project
```

To change the Scalafmt version,

```scala
scalafmtVersion := "0.7.0-RC1"
// this version can even be changed per-project, if desired
```

By default, Scalafmt runs before compiling. You can change that with

```scala
scalafmtOnCompile := false
```

## Implementation details

Loading Scalafmt in a separate classloader allows sbt-scalafmt to work across sbt and Scalafmt versions, regardless of
the Scala versions used.

Scalafmt artifacts are downloaded with a scalafmt Ivy configuration.
