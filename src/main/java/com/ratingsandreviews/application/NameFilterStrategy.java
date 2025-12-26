package com.ratingsandreviews.application;

import java.util.List;

public class NameFilterStrategy implements FilterStrategy {
    @Override
    public List<Application> filter(ApplicationRepository repository, String filterValue) {
        return repository.findByNameContainingIgnoreCase(filterValue);
    }
}

