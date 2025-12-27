package com.ratingsandreviews.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.UUID;

public interface ApplicationRatingStatsRepository extends JpaRepository<ApplicationRatingStats, Pair<UUID, Integer>> {
    List<ApplicationRatingStats> findByIdApplicationId(UUID applicationId);
}