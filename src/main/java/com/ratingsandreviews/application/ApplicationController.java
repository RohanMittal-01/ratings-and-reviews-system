package com.ratingsandreviews.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;;

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
    public List<Application> getApplications(@RequestParam(required = false) String filterKey, @RequestParam(required = false) String filterValue, @RequestParam (required = false) String sort, @RequestParam(required = false) String order, @RequestParam (required = false) Integer page, @RequestParam (required = false) Integer size) {
        return this.applicationService.getApplications(filterKey, filterKey, sort, order, page, size);
    }
}
