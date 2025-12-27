package com.ratingsandreviews.rating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RatingServiceImpl implements RatingService {
    private final RatingRepositoryWrapper ratingRepositoryWrapper;
    private final static Double DEFAULT_RATING = 0.0;

    @Autowired
    public RatingServiceImpl(RatingRepositoryWrapper ratingRepositoryWrapper) {
        this.ratingRepositoryWrapper = ratingRepositoryWrapper;
    }

    @Override
    public Double getRatingForApplication(String applicationId) {
        if (applicationId == null || applicationId.isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }
        // TODO :: do we need to check if application exists?
        Double rating = this.ratingRepositoryWrapper.getAvgByApplicationId(applicationId);
        if(rating == null) {
            rating = DEFAULT_RATING;
        }
        return rating;
    }

    @Override
    public List<ApplicationRatingStats> getCategoryStatsForApplication(String applicationId) {
        if (applicationId == null || applicationId.isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }
        UUID uuid = UUID.fromString(applicationId);
        return this.ratingRepositoryWrapper.getCategoryStatsForApplication(uuid);
    }

    @Override
    public Page<Rating> getRatingsByApplicationId(UUID applicationId, Pageable pageable) {
        return ratingRepositoryWrapper.getRatingsByApplicationId(applicationId, pageable);
    }

    @Override
    public Rating submitRating(Rating rating) {
        return ratingRepositoryWrapper.saveRating(rating);
    }
}