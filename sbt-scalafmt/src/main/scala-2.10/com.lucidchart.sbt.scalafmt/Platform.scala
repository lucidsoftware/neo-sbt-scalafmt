package com.lucidchart.sbt.scalafmt

import sbinary.DefaultProtocol._
import sbt._
import sbt.FileInfo.full.{format => hashModifiedFormat}
import sbt.inc.Analysis

object AnalysisPlatform {
  def counted(prefix: String, single: String, plural: String, count: Int) =
    Analysis.counted(prefix, single, plural, count)
}

object CachePlatform {
  def fileInfo(file: File, hash: List[Byte], lastModified: Long) = FileInfo.full.make(file, hash, lastModified)

  def readFileInfo(cache: File) = CacheIO.fromFile[Set[HashModifiedFileInfo]](cache).getOrElse(Set.empty)

  def writeFileInfo(cache: File, value: Set[HashModifiedFileInfo]) = CacheIO.toFile(value)(cache)
}

object LibraryPlatform {
  def filterConfiguration(update: UpdateReport, configuration: Configuration) =
    update.select(configurationFilter(configuration.name))

  def withOverrideScalaVersion(ivyScala: IvyScala, value: Boolean) = ivyScala.copy(overrideScalaVersion = value)
}
