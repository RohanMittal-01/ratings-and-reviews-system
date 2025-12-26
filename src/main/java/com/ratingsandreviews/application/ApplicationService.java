package com.ratingsandreviews.application;

import java.util.List;

public interface ApplicationService {
    Application getApplication(String applicationId);
    List<Application> getApplications(String filterKey, String filterValue, String sort, String order, Integer page, Integer size);
    void installApplication(String applicationId);
    void installApplications(List<String> applicationIds);
    void uninstallApplication(String applicationId);
    void uninstallApplications(List<String> applicationIds);
}