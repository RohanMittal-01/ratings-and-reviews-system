package com.ratingsandreviews.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.ratingsandreviews.util.Validations.validateOptionalExistence;

@Component
public class ApplicationRepositoryWrapper {
    private final ApplicationRepository applicationRepository;

    @Autowired
    public ApplicationRepositoryWrapper(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    Application getApplication(String applicationId) {
        UUID uuid = UUID.fromString(applicationId);
        return validateOptionalExistence(this.applicationRepository.findById(uuid), Application.class, uuid.toString());
    }

    Page<Application> getApplications(String filterKey, String filterValue, Pageable pageable) {
        FilterStrategy strategy = FilterStrategyFactory.getStrategy(filterKey);
        return strategy.filter(this.applicationRepository, filterValue, pageable);
    }
}