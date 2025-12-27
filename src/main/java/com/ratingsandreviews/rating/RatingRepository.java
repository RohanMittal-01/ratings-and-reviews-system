package com.ratingsandreviews.rating;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Page<Rating> findByApplicationId(UUID applicationId, Pageable pageable);
}