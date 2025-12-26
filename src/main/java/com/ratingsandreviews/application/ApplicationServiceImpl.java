package com.ratingsandreviews.application;

import com.ratingsandreviews.util.AppLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 50;
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
    public List<Application> getApplications(String filterKey, String filterValue, String sort, String order, Integer page, Integer size) {
        if(page == null) page = 0;
        if(size == null) size = DEFAULT_PAGE_SIZE;
        if(size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if(sort == null) sort = "updatedAt";
        if(order == null) order = "desc";
        return this.applicationRepository.getApplications(filterKey, filterValue, sort, order, page, size);
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
