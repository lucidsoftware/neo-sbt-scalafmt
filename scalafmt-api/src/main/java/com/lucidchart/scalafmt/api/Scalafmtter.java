package com.lucidchart.scalafmt.api;

public interface Scalafmtter {
    Object config(String string);

    String format(Object config, String code);
}
