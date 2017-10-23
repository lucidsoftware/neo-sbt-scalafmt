package com.lucidchart.scalafmt.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A very simple LRU cache based an a JVM collection type.
 *
 * http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/
 */

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private int cacheSize;

    public LRUCache(int cacheSize) {
        // Magic happens here: the third parameter specifies that the keys are ordered on access not insertion.
        super(16, 0.75f, true);
        this.cacheSize = cacheSize;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= cacheSize;
    }
}