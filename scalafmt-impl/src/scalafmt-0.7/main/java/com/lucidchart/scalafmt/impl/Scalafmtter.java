package com.lucidchart.scalafmt.impl;

import org.scalafmt.Scalafmt;
import org.scalafmt.config.Config;
import org.scalafmt.config.ScalafmtConfig;
import scala.Option;
import scala.collection.immutable.Set$;
import scala.meta.internal.tokenizers.PlatformTokenizerCache;

public final class Scalafmtter implements com.lucidchart.scalafmt.api.Scalafmtter {
    public Object config(String string) {
        return Config.fromHoconString(string, Option.empty()).get();
    }

    // Otherwise, this cache hangs on to a lot
    public void finalize() throws Throwable {
        try {
            PlatformTokenizerCache.megaCache().clear();
        } finally {
            super.finalize();
        }
    }

    public String format(Object config, String code) {
        return Scalafmt.format(code, (ScalafmtConfig)config, Set$.MODULE$.empty()).get();
    }
}
