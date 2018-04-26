package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import com.lucidchart.scalafmt.api.Dialect
import java.nio.file.Path
import java.util.function
import org.scalafmt
import org.scalafmt.config.ScalafmtConfig
import scala.meta.dialects

class Scalafmtter(config: ScalafmtConfig) extends api.Scalafmtter {
  self =>

  def formatter(dialect: Dialect) = new function.Function[String, String] {
    private[this] val config = dialect match {
      case Dialect.SBT => scalafmt.Scalafmt.configWithDialect(self.config, dialects.Sbt0137)
      case Dialect.SCALA => self.config
    }
    def apply(code: String) = scalafmt.Scalafmt.format(code, config).get
  }

  def includeFile(file: Path) = config.project.matcher.matches(file.toString)

  // Otherwise, this cache hangs on to a lot
  override protected def finalize() = try ScalametaUtil.clearCache() finally super.finalize()

}
