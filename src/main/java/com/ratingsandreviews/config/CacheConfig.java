package com.ratingsandreviews.config;

import com.ratingsandreviews.cache.CacheFactory;
import com.ratingsandreviews.cache.CacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Order(1) // Ensure this config is created early
public class CacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();

        // Initialize cache factory with Redis
        CacheFactory.initialize(template);

        return template;
    }

    @Bean
    @DependsOn("redisTemplate")
    public CacheService redisCacheService() {
        return CacheFactory.getRedisCache();
    }

    @Bean
    public CacheService caffeineCacheService() {
        return CacheFactory.getCaffeineCache();
    }
}
