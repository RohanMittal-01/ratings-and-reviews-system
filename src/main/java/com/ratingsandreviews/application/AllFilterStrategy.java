package com.ratingsandreviews.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class AllFilterStrategy implements FilterStrategy {
    @Override
    public Page<Application> filter(ApplicationRepository repository, String filterValue, Pageable pageable) {
        return repository.findAll(pageable);
    }
}