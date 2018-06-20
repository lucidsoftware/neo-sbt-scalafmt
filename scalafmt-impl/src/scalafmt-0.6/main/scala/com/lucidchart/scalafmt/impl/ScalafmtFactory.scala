package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import java.util.function
import org.scalafmt.config.Config
import scala.meta.dialects

final class ScalafmtFactory extends api.ScalafmtFactory {

  def fromConfig(configString: String) = Config.fromHocon(configString, Option.empty) match {
    case Left(message) => throw new IllegalArgumentException(message)
    case Right(config) => new Scalafmtter(config)
  }

}
