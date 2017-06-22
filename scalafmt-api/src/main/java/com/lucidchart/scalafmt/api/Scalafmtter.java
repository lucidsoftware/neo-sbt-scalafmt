package com.lucidchart.scalafmt.api;

import java.nio.file.Path;
import java.util.function.Function;

public interface Scalafmtter {

    Function<String, String> formatter(Dialect dialect);

    boolean includeFile(Path file);

}
