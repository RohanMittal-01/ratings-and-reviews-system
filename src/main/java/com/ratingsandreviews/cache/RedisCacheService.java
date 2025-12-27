package com.ratingsandreviews.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RedisCacheService implements CacheService {
    private static volatile RedisCacheService instance;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final long DEFAULT_TTL = 3600; // 1 hour in seconds

    private RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public static RedisCacheService getInstance(RedisTemplate<String, Object> redisTemplate) {
        if (instance == null) {
            synchronized (RedisCacheService.class) {
                if (instance == null) {
                    instance = new RedisCacheService(redisTemplate);
                }
            }
        }
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                if (type.isInstance(value)) {
                    return (T) value;
                }
                // Try to convert if needed
                return objectMapper.convertValue(value, type);
            }
        } catch (Exception e) {
            // Log error and return null
            System.err.println("Error getting from Redis cache: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    public void put(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error putting to Redis cache: " + e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Error evicting from Redis cache: " + e.getMessage());
        }
    }

    @Override
    public void evictPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            System.err.println("Error evicting pattern from Redis cache: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            System.err.println("Error clearing Redis cache: " + e.getMessage());
        }
    }

    @Override
    public String getCacheType() {
        return "REDIS";
    }
}
