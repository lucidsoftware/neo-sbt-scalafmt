package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import com.lucidchart.scalafmt.api.Dialect
import java.util.function
import org.scalafmt.config.Config
import scala.meta.dialects

final class ScalafmtFactory extends api.ScalafmtFactory {

  def fromConfig(configString: String) = {
    val config = Config.fromHocon(configString, Option.empty).right.get
    new function.Function[Dialect, api.Scalafmtter] {
      def apply(dialect: Dialect) = new Scalafmtter(dialect match {
        case Dialect.SBT => config.copy(runner = config.runner.copy(dialect = dialects.Sbt0137))
        case Dialect.SCALA => config
      })
    }
  }

}
