package com.lucidchart.sbt.scalafmt

import java.net.{URL, URLClassLoader}

class BridgeClassLoader(classpath: Seq[URL])(include: String => Boolean)
    extends URLClassLoader(classpath.toArray, null) {

  private[this] val otherClassLoader = getClass.getClassLoader

  override def findClass(name: String) =
    if (include(name)) super.findClass(name) else otherClassLoader.loadClass(name)

}
