package com.lucidchart.scalafmt.impl;

import scala.meta.internal.tokenizers.ScalametaTokenizer$;

public final class ScalametaUtil {

    private ScalametaUtil() {
    }

    public static void clearCache() {
        ScalametaTokenizer$.MODULE$.scala$meta$internal$tokenizers$ScalametaTokenizer$$megaCache().clear();
    }

}
