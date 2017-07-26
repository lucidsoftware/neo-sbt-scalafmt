package com.lucidchart.sbt.scalafmt

import com.lucidchart.scalafmt.api.Scalafmtter
import java.io.File
import sbt.FileFilter

class ScalafmtFileFilter(scalafmtter: Scalafmtter) extends FileFilter {
  def accept(file: File) = file.isFile && scalafmtter.includeFile(file.toPath)
}
