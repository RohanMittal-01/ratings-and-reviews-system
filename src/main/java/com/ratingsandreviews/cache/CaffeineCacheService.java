package com.ratingsandreviews.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public class CaffeineCacheService implements CacheService {
    private static volatile CaffeineCacheService instance;
    private final Cache<String, Object> cache;

    private CaffeineCacheService() {
        // LFU eviction policy with max size of 10,000 entries
        // TTL of 30 minutes for entries
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    public static CaffeineCacheService getInstance() {
        if (instance == null) {
            synchronized (CaffeineCacheService.class) {
                if (instance == null) {
                    instance = new CaffeineCacheService();
                }
            }
        }
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = cache.getIfPresent(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public void evict(String key) {
        cache.invalidate(key);
    }

    @Override
    public void evictPattern(String pattern) {
        // Support both prefix matching and wildcard patterns
        if (pattern.contains("*")) {
            // Convert pattern with wildcards to regex
            String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
            cache.asMap().keySet().stream()
                .filter(key -> key.matches(regex))
                .forEach(cache::invalidate);
        } else {
            // Simple prefix matching (faster)
            cache.asMap().keySet().stream()
                .filter(key -> key.startsWith(pattern))
                .forEach(cache::invalidate);
        }
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public String getCacheType() {
        return "CAFFEINE";
    }

    public Cache<String, Object> getCache() {
        return cache;
    }
}
