package com.lucidchart.scalafmt.impl

import com.lucidchart.scalafmt.api
import org.scalafmt.Scalafmt
import org.scalafmt.config.ScalafmtConfig

final class Scalafmtter(config: ScalafmtConfig) extends api.Scalafmtter {

  // Otherwise, this cache hangs on to a lot
  override protected def finalize() = try ScalametaUtil.clearCache() finally super.finalize()

  def format(code: String) = Scalafmt.format(code, config, Set.empty).get

}
