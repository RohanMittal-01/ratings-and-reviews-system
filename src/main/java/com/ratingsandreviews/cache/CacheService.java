package com.ratingsandreviews.cache;

public interface CacheService {
    <T> T get(String key, Class<T> type);
    void put(String key, Object value);
    void evict(String key);
    void evictPattern(String pattern);
    void clear();
    String getCacheType();
}
