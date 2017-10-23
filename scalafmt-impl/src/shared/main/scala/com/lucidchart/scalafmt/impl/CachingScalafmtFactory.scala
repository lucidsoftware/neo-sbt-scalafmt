package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api

trait CachingScalafmtFactory extends api.ScalafmtFactory {

  private val cachedFormatters = new LRUCache[String, Scalafmtter](256)

  def buildScalafmtterFromConfig(configString: String): Scalafmtter

  override def fromConfig(configString: String): api.Scalafmtter =
    Option(cachedFormatters.get(configString)).getOrElse {
      val formatter = buildScalafmtterFromConfig(configString)
      cachedFormatters.put(configString, formatter)
    }
}
