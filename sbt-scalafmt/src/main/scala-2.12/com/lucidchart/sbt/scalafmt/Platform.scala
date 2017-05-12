package com.lucidchart.sbt.scalafmt

import sbt._
import sbt.internal.inc.Analysis
import sbt.librarymanagement.IvyScala
import sbt.util.CacheImplicits._
import sbt.util.CacheStore
import scala.reflect.runtime.universe

object AnalysisPlatform {
  def counted(prefix: String, single: String, plural: String, count: Int) =
    Analysis.counted(prefix, single, plural, count)
}

object CachePlatform {
  private[this] val mirror = universe.runtimeMirror(getClass.getClassLoader)

  private[this] val fileHashModified = {
    val module = mirror.reflectModule(mirror.staticModule("sbt.util.FileHashModified"))
    mirror.reflect(module.instance).reflectMethod(module.symbol.info.decl(universe.TermName("apply")).asMethod)
  }

  def fileInfo(file: File, hash: List[Byte], lastModified: Long) =
    fileHashModified(file, hash, lastModified.asInstanceOf[AnyRef]).asInstanceOf[HashModifiedFileInfo]

  def readFileInfo(cache: File) = CacheStore(cache).read(Set.empty[HashModifiedFileInfo])

  def writeFileInfo(cache: File, value: Set[HashModifiedFileInfo]) = CacheStore(cache).write(value)
}

object LibraryPlatform {
  def withOverrideScalaVersion(ivyScala: IvyScala, value: Boolean) = ivyScala.withOverrideScalaVersion(value)
}
