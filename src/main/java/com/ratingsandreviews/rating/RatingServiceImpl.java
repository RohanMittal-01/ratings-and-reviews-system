package com.ratingsandreviews.rating;

import com.ratingsandreviews.cache.CacheKeyBuilder;
import com.ratingsandreviews.cache.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RatingServiceImpl implements RatingService {
    private final RatingRepositoryWrapper ratingRepositoryWrapper;
    private final CacheService redisCache;
    private final static Double DEFAULT_RATING = 0.0;

    @Autowired
    public RatingServiceImpl(RatingRepositoryWrapper ratingRepositoryWrapper, CacheService redisCacheService) {
        this.ratingRepositoryWrapper = ratingRepositoryWrapper;
        this.redisCache = redisCacheService;
    }

    @Override
    public Double getRatingForApplication(String applicationId) {
        if (applicationId == null || applicationId.isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }

        // Check cache first
        String cacheKey = CacheKeyBuilder.ratingAvgKey(applicationId);
        Double cached = redisCache.get(cacheKey, Double.class);
        if (cached != null) {
            return cached;
        }

        // Fetch from DB and cache
        Double rating = this.ratingRepositoryWrapper.getAvgByApplicationId(applicationId);
        if(rating == null) {
            rating = DEFAULT_RATING;
        }

        redisCache.put(cacheKey, rating);
        return rating;
    }

    @Override
    public List<ApplicationRatingStats> getCategoryStatsForApplication(String applicationId) {
        if (applicationId == null || applicationId.isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }

        // Check cache first
        String cacheKey = CacheKeyBuilder.ratingStatsKey(applicationId);
        @SuppressWarnings("unchecked")
        List<ApplicationRatingStats> cached = redisCache.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        UUID uuid = UUID.fromString(applicationId);
        List<ApplicationRatingStats> stats = this.ratingRepositoryWrapper.getCategoryStatsForApplication(uuid);

        redisCache.put(cacheKey, stats);
        return stats;
    }

    @Override
    public Page<Rating> getRatingsByApplicationId(UUID applicationId, Pageable pageable) {
        // Cache paginated ratings
        String cacheKey = CacheKeyBuilder.ratingsPageKey(applicationId.toString(), pageable.getPageNumber(), pageable.getPageSize());
        @SuppressWarnings("unchecked")
        Page<Rating> cached = redisCache.get(cacheKey, Page.class);
        if (cached != null) {
            return cached;
        }

        Page<Rating> page = ratingRepositoryWrapper.getRatingsByApplicationId(applicationId, pageable);
        redisCache.put(cacheKey, page);
        return page;
    }

    @Override
    public Rating submitRating(Rating rating) {
        Rating saved = ratingRepositoryWrapper.saveRating(rating);

        // Evict all rating-related caches for this application
        redisCache.evictPattern(CacheKeyBuilder.ratingsPattern(saved.getApplicationId().toString()));

        return saved;
    }
}