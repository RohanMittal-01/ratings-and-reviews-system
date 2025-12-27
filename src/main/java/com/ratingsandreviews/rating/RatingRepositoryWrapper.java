package com.ratingsandreviews.rating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RatingRepositoryWrapper {
    private final RatingRepository ratingRepository;
    private final ApplicationRatingStatsRepository statsRepository;

    @Autowired
    public RatingRepositoryWrapper(RatingRepository ratingRepository, ApplicationRatingStatsRepository statsRepository) {
        this.ratingRepository = ratingRepository;
        this.statsRepository = statsRepository;
    }

    public Double getAvgByApplicationId(String applicationId) {
        UUID uuid = UUID.fromString(applicationId);
        Double avgRating = null;
        List<ApplicationRatingStats> applicationRatingStats = this.getCategoryStatsForApplication(uuid);
        if (!applicationRatingStats.isEmpty()) {
            long totalSum = 0L;
            long totalRatings = 0L;
            for(ApplicationRatingStats stats : applicationRatingStats) {
                if (avgRating == null) {
                    avgRating = 0.0;
                }
                totalSum += stats.getId().getScale() * stats.getCount();
                totalRatings += stats.getCount();
            }
            avgRating = (double) (totalSum / totalRatings);
        }
        return avgRating;
    }

    public List<ApplicationRatingStats> getCategoryStatsForApplication(UUID applicationId) {
        return statsRepository.findByIdApplicationId(applicationId);
    }

    public Rating saveRating(Rating rating) {
        return this.ratingRepository.save(rating);
    }

    public Page<Rating> getRatingsByApplicationId(UUID applicationId, Pageable pageable) {
        return ratingRepository.findByApplicationId(applicationId, pageable);
    }
}