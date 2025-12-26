package com.ratingsandreviews.application;

import org.springframework.stereotype.Service;

import java.util.List;

public interface ApplicationService {
    Application getApplication(String applicationId);
    List<Application> getApplications(String filterKey, String filterValue, String sort, String order, Integer page, Integer size);
}