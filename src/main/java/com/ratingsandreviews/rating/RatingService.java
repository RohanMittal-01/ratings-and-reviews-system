package com.ratingsandreviews.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RatingService {
    Double getRatingForApplication(String applicationId);
    List<ApplicationRatingStats> getCategoryStatsForApplication(String applicationId);
    Page<Rating> getRatingsByApplicationId(UUID applicationId, Pageable pageable);
    Rating submitRating(Rating rating);
}