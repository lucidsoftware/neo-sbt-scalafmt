package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import com.lucidchart.scalafmt.api.{Dialect, Scalafmtter}
import java.util.function
import org.scalafmt.config.Config
import scala.meta.dialects

final class ScalafmtFactory extends api.ScalafmtFactory {

  def fromConfig(configString: String) = new Scalafmtter(Config.fromHocon(configString, Option.empty).right.get)

}
