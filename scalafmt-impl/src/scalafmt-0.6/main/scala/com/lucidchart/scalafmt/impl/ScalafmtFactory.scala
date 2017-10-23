package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import com.lucidchart.scalafmt.api.LRUCache
import org.scalafmt.config.Config

final class ScalafmtFactory extends api.ScalafmtFactory {

  private val cachedFormatters = new LRUCache[String, Scalafmtter](100)

  def fromConfig(configString: String) = {
    Option(cachedFormatters.get(configString)).getOrElse {
      val formatter = new Scalafmtter(Config.fromHocon(configString, Option.empty).right.get)
      cachedFormatters.put(configString, formatter)
    }
  }

}
