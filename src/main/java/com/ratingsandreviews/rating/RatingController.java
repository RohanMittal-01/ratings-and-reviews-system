package com.ratingsandreviews.rating;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/ratings")
public class RatingController {
    private final RatingService ratingService;

    @Autowired
    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/{applicationId}")
    public Page<Rating> getRatingsByApplicationId(
            @PathVariable UUID applicationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ratingService.getRatingsByApplicationId(applicationId, pageable);
    }

    @GetMapping("/average/{applicationId}")
    public Double getAverageRatingForApplication(@PathVariable String applicationId) {
        return ratingService.getRatingForApplication(applicationId);
    }

    @GetMapping("/category-stats/{applicationId}")
    public List<ApplicationRatingStats> getCategoryStatsForApplication(@PathVariable String applicationId) {
        return ratingService.getCategoryStatsForApplication(applicationId);
    }

    @PostMapping
    public ResponseEntity<RatingSubmissionResponse> submitRating(@RequestBody Rating rating) {
        Rating saved = ratingService.submitRating(rating);
        Double avg = ratingService.getRatingForApplication(saved.getApplicationId().toString());
        List<ApplicationRatingStats> stats = ratingService.getCategoryStatsForApplication(saved.getApplicationId().toString());
        // Return the first page of ratings (page 0, size 10, sorted by updatedAt desc)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());
        Page<Rating> page = ratingService.getRatingsByApplicationId(saved.getApplicationId(), pageable);
        RatingSubmissionResponse response = new RatingSubmissionResponse(saved, avg, stats, page);
        return ResponseEntity.ok(response);
    }

    public static class RatingSubmissionResponse {
        private final Rating savedRating;
        private final Double newAverage;
        private final List<ApplicationRatingStats> newCategoryStats;
        private final Page<Rating> page;

        public RatingSubmissionResponse(Rating savedRating, Double newAverage, List<ApplicationRatingStats> newCategoryStats, Page<Rating> page) {
            this.savedRating = savedRating;
            this.newAverage = newAverage;
            this.newCategoryStats = newCategoryStats;
            this.page = page;
        }
        public Rating getSavedRating() { return savedRating; }
        public Double getNewAverage() { return newAverage; }
        public List<ApplicationRatingStats> getNewCategoryStats() { return newCategoryStats; }
        public Page<Rating> getPage() { return page; }
    }
}
