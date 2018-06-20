package com.lucidchart.scalafmt.impl

import org.scalafmt
import org.scalafmt.config.ScalafmtConfig
import scala.meta.Dialect

object ScalafmtConfigUtil {

  def setDialect(config: ScalafmtConfig, dialect: Dialect) =
    config.copy(runner = config.runner.copy(dialect = dialect))

}

