package com.lucidchart.scalafmt.impl;

import org.scalafmt.Scalafmt;
import org.scalafmt.config.Config;
import org.scalafmt.config.ScalafmtConfig;
import scala.Option;
import scala.collection.immutable.Set$;

public final class Scalafmtter implements com.lucidchart.scalafmt.api.Scalafmtter {
    public Object config(String string) {
        return Config.fromHocon(string, Option.empty()).right().get();
    }

    public String format(Object config, String code) {
        return Scalafmt.format(code, (ScalafmtConfig)config, Set$.MODULE$.empty()).get();
    }
}
