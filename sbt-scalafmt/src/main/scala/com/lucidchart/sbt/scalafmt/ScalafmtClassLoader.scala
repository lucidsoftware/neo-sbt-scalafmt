package com.lucidchart.sbt.scalafmt

import java.io.File
import java.net.URLClassLoader
import scala.collection.breakOut

class ScalafmtClassLoader(classpath: Seq[File]) extends URLClassLoader(classpath.map(_.toURI.toURL)(breakOut), null) {

  override def findClass(name: String) =
    if (name.startsWith("com.lucidchart.scalafmt.api.")) {
      getClass.getClassLoader.loadClass(name)
    } else {
      super.findClass(name)
    }

}
