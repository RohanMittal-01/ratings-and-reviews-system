package com.ratingsandreviews.rating;

import java.util.UUID;

public class RatingBuilder {
    private UUID applicationId;
    private int rating;
    private UUID id;
    public RatingBuilder id(UUID id) {
        this.id = id;
        return this;
    }
    public RatingBuilder rating(int rating) {
        this.rating = rating;
        return this;
    }
    public RatingBuilder applicationId(UUID applicationId) {
        this.applicationId = applicationId;
        return this;
    }
    public Rating build() {
        Rating ratingObj = new Rating();
        ratingObj.setId(this.id);
        ratingObj.setRating(this.rating);
        ratingObj.setApplicationId(this.applicationId);
        return ratingObj;
    }
}




