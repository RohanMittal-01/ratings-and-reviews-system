package com.ratingsandreviews.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{applicationId}")
    public Application getApplication(@PathVariable String applicationId) {
        return this.applicationService.getApplication(applicationId);
    }

    @GetMapping
    public Page<Application> getApplications(@RequestParam(required = false) String filterKey,
                                             @RequestParam(required = false) String filterValue,
                                             @RequestParam (defaultValue = "updatedAt") String sortBy,
                                             @RequestParam(defaultValue = "desc") String order,
                                             @RequestParam (defaultValue = "0") Integer page,
                                             @RequestParam (defaultValue = "10") Integer size) {
        Sort sort = order.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return this.applicationService.getApplications(filterKey, filterValue, pageable);
    }

    @PostMapping(value = "/install")
    public void installApplication(@RequestBody ApplicationIdRequest applicationIdRequest) {
        String applicationId = applicationIdRequest.applicationId();
        this.applicationService.installApplication(applicationId);
    }

    @PostMapping(value = "/install/batch")
    public void installApplications(@RequestBody ApplicationIdsRequest applicationIdsRequest) {
        List<String> applicationIds = applicationIdsRequest.applicationIds();
        this.applicationService.installApplications(applicationIds);
    }

    @PostMapping(value = "/uninstall")
    public void uninstallApplication(@RequestBody ApplicationIdRequest applicationIdRequest) {
        String applicationId = applicationIdRequest.applicationId();
        this.applicationService.uninstallApplication(applicationId);
    }

    @PostMapping(value = "/uninstall/batch")
    public void uninstallApplications(@RequestBody ApplicationIdsRequest applicationIdsRequest) {
        List<String> applicationIds = applicationIdsRequest.applicationIds();
        this.applicationService.uninstallApplications(applicationIds);
    }

    public record ApplicationIdRequest(String applicationId) { }
    public record ApplicationIdsRequest(List<String> applicationIds) { }
}
