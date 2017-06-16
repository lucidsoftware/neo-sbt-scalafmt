package com.lucidchart.scalafmt.api;

import java.util.function.Function;

public interface ScalafmtFactory {

    Function<Dialect, Scalafmtter> fromConfig(String configString);

}
