package com.ratingsandreviews.rating;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity(name = "application_rating_stats")
@Data
public class ApplicationRatingStats {
    @EmbeddedId
    private ApplicationRatingStatsId id;
    private Long count;
}