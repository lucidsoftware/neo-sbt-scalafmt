package com.lucidchart.scalafmt.api;

public interface ScalafmtFactory {

    Scalafmtter fromConfig(String configString);

}
