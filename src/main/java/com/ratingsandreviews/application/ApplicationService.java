package com.ratingsandreviews.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {
    Application getApplication(String applicationId);
    Page<Application> getApplications(String filterKey, String filterValue, Pageable pageable);
    void installApplication(String applicationId);
    void installApplications(List<String> applicationIds);
    void uninstallApplication(String applicationId);
    void uninstallApplications(List<String> applicationIds);
}