package com.lucidchart.scalafmt.impl

import scala.meta.internal.tokenizers.PlatformTokenizerCache

object ScalametaUtil {

  def clearCache() = PlatformTokenizerCache.megaCache.clear()

}
