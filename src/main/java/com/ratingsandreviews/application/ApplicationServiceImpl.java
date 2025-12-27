package com.ratingsandreviews.application;

import com.ratingsandreviews.util.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final AppLogger logger = AppLogger.getInstance(ApplicationServiceImpl.class);

    private final ApplicationRepositoryWrapper applicationRepository;

    @Autowired
    public ApplicationServiceImpl(ApplicationRepositoryWrapper applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Application getApplication(String applicationId) {
        return this.applicationRepository.getApplication(applicationId);
    }

    @Override
    public Page<Application> getApplications(String filterKey, String filterValue, Pageable pageable) {
        return this.applicationRepository.getApplications(filterKey, filterValue, pageable);
    }

    @Override
    public void installApplication(String applicationId) {
        // For now, just log the installation
        logger.info("Installing application with ID: " + applicationId);
    }

    @Override
    public void installApplications(List<String> applicationIds) {
        // use parallel stream for better performance
        applicationIds.parallelStream().forEach(this::installApplication);
    }

    @Override
    public void uninstallApplication(String applicationId) {
        // For now, just log the uninstallation
        logger.info("Uninstalling application with ID: " + applicationId);
    }

    @Override
    public void uninstallApplications(List<String> applicationIds) {
        // use parallel stream for better performance
        applicationIds.parallelStream().forEach(this::uninstallApplication);
    }
}
