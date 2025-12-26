package com.ratingsandreviews.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ratingsandreviews.util.Validations.validateOptionalExistence;

@Service
public class ApplicationRepositoryWrapper {
    private final ApplicationRepository applicationRepository;

    @Autowired
    public ApplicationRepositoryWrapper(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    Application getApplication(String applicationId) {
        UUID uuid = UUID.fromString(applicationId);
        Optional<Application> applicationOptional = this.applicationRepository.findById(uuid);
        validateOptionalExistence(applicationOptional, Application.class, uuid.toString());
        return this.applicationRepository.findById(UUID.fromString(applicationId)).orElse(null);
    }

    List<Application> getApplications(String filterKey, String filterValue, String sort, String order, Integer page, Integer size) {
        FilterStrategy strategy = FilterStrategyFactory.getStrategy(filterKey);
        return strategy.filter(this.applicationRepository, filterValue);
    }
}
