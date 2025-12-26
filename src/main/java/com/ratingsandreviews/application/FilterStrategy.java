package com.ratingsandreviews.application;

import java.util.List;

public interface FilterStrategy {
    List<Application> filter(ApplicationRepository repository, String filterValue);
}