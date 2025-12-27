package com.ratingsandreviews.cache;

import org.springframework.data.redis.core.RedisTemplate;

public class CacheFactory {
    private static CaffeineCacheService caffeineCache;
    private static RedisCacheService redisCache;
    private static volatile RedisTemplate<String, Object> redisTemplate;
    private static volatile boolean initialized = false;

    public enum CacheType {
        CAFFEINE,
        REDIS
    }

    public static synchronized void initialize(RedisTemplate<String, Object> template) {
        if (!initialized) {
            redisTemplate = template;
            caffeineCache = CaffeineCacheService.getInstance();
            if (redisTemplate != null) {
                redisCache = RedisCacheService.getInstance(redisTemplate);
            }
            initialized = true;
        }
    }

    public static CacheService getCache(CacheType type) {
        if (type == CacheType.CAFFEINE) {
            if (caffeineCache == null) {
                synchronized (CacheFactory.class) {
                    if (caffeineCache == null) {
                        caffeineCache = CaffeineCacheService.getInstance();
                    }
                }
            }
            return caffeineCache;
        } else {
            // Wait for initialization if not done yet
            int maxRetries = 50; // 5 seconds total
            int retries = 0;
            while (!initialized && retries < maxRetries) {
                try {
                    Thread.sleep(100);
                    retries++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (redisCache == null && redisTemplate != null) {
                synchronized (CacheFactory.class) {
                    if (redisCache == null && redisTemplate != null) {
                        redisCache = RedisCacheService.getInstance(redisTemplate);
                    }
                }
            }
            if (redisCache == null) {
                throw new IllegalStateException("Redis cache not initialized. RedisTemplate is not available. Make sure Redis is running and CacheConfig bean is created.");
            }
            return redisCache;
        }
    }

    public static CacheService getCaffeineCache() {
        return getCache(CacheType.CAFFEINE);
    }

    public static CacheService getRedisCache() {
        return getCache(CacheType.REDIS);
    }
}
