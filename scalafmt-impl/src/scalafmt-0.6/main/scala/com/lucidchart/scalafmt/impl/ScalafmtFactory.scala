package com.lucidchart.scalafmt.impl

import org.scalafmt.config.Config

final class ScalafmtFactory extends CachingScalafmtFactory {

  override def buildScalafmtterFromConfig(configString: String) =
    new Scalafmtter(Config.fromHocon(configString, Option.empty).right.get)
}
