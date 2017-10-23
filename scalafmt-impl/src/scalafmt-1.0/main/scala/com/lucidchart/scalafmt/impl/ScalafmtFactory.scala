package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import java.util.function
import org.scalafmt.config.Config
import scala.meta.dialects

final class ScalafmtFactory extends CachingScalafmtFactory {

  override def buildScalafmtterFromConfig(configString: String) =
    new Scalafmtter(Config.fromHoconString(configString, Option.empty).get)

}
