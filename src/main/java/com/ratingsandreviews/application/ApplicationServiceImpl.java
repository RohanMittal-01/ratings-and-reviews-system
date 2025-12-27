package com.ratingsandreviews.application;

import com.ratingsandreviews.cache.CacheKeyBuilder;
import com.ratingsandreviews.cache.CacheService;
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
    private final CacheService redisCache;

    @Autowired
    public ApplicationServiceImpl(ApplicationRepositoryWrapper applicationRepository, CacheService redisCacheService) {
        this.applicationRepository = applicationRepository;
        this.redisCache = redisCacheService;
    }

    @Override
    public Application getApplication(String applicationId) {
        // Check cache first
        String cacheKey = CacheKeyBuilder.applicationKey(applicationId);
        Application cached = redisCache.get(cacheKey, Application.class);
        if (cached != null) {
            return cached;
        }

        Application application = this.applicationRepository.getApplication(applicationId);
        if (application != null) {
            redisCache.put(cacheKey, application);
        }
        return application;
    }

    @Override
    public Page<Application> getApplications(String filterKey, String filterValue, Pageable pageable) {
        // Cache paginated application lists
        String cacheKey = CacheKeyBuilder.applicationsPageKey(filterKey, filterValue, pageable.getPageNumber(), pageable.getPageSize());
        @SuppressWarnings("unchecked")
        Page<Application> cached = redisCache.get(cacheKey, Page.class);
        if (cached != null) {
            return cached;
        }

        Page<Application> page = this.applicationRepository.getApplications(filterKey, filterValue, pageable);
        redisCache.put(cacheKey, page);
        return page;
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
