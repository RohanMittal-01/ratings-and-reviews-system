package com.ratingsandreviews.application;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ApplicationRepositoryWrapper applicationRepository;

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
}
