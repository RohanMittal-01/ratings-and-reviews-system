package com.ratingsandreviews.rating;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RatingServiceImplTest {
    @Mock
    private RatingRepositoryWrapper wrapper;
    @InjectMocks
    private RatingServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitRating_savesAndReturns() {
        Rating rating = new Rating();
        when(wrapper.saveRating(any(Rating.class))).thenReturn(rating);
        Rating saved = service.submitRating(rating);
        assertNotNull(saved);
        verify(wrapper).saveRating(rating);
    }

    @Test
    void getRatingsByApplicationId_returnsPage() {
        UUID appId = UUID.randomUUID();
        Page<Rating> page = new PageImpl<>(List.of(new Rating()));
        when(wrapper.getRatingsByApplicationId(eq(appId), any(Pageable.class))).thenReturn(page);
        Page<Rating> result = service.getRatingsByApplicationId(appId, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getRatingForApplication_returnsDefaultIfNull() {
        String appId = UUID.randomUUID().toString();
        when(wrapper.getAvgByApplicationId(appId)).thenReturn(null);
        Double result = service.getRatingForApplication(appId);
        assertEquals(0.0, result);
    }

    @Test
    void getCategoryStatsForApplication_returnsList() {
        String appId = UUID.randomUUID().toString();
        List<ApplicationRatingStats> stats = List.of(new ApplicationRatingStats());
        when(wrapper.getCategoryStatsForApplication(any(UUID.class))).thenReturn(stats);
        List<ApplicationRatingStats> result = service.getCategoryStatsForApplication(appId);
        assertThat(result).hasSize(1);
    }
}
