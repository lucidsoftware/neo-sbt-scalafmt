package com.lucidchart.scalafmt.impl

import org.scalafmt.Scalafmt
import org.scalafmt.config.ScalafmtConfig
import scala.meta.Dialect

object ScalafmtConfigUtil {

  def setDialect(config: ScalafmtConfig, dialect: Dialect) =
    Scalafmt.configWithDialect(config, dialect)

}
