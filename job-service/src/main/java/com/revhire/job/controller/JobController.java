package com.revhire.job.controller;

import com.revhire.job.dto.CreateJobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.dto.PageResponse;
import com.revhire.job.dto.UpdateJobRequest;
import com.revhire.job.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Validated
public class JobController {
    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody CreateJobRequest request,
            Authentication authentication
    ) {
        JobResponse created = service.create(request, recruiterId(authentication));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.jobId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public JobResponse getById(@PathVariable @Positive Long id) {
        return service.getById(id);
    }

    @GetMapping
    public PageResponse<JobResponse> search(@Valid @ModelAttribute JobSearchRequest request) {
        return service.search(request);
    }

    @GetMapping("/recruiter/me")
    public List<JobResponse> getRecruiterJobs(Authentication authentication) {
        return service.getRecruiterJobs(recruiterId(authentication));
    }

    @PatchMapping("/{id}")
    public JobResponse partiallyUpdate(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateJobRequest request,
            Authentication authentication
    ) {
        return service.update(id, recruiterId(authentication), request);
    }

    @PatchMapping("/{id}/close")
    public JobResponse close(@PathVariable @Positive Long id, Authentication authentication) {
        return service.close(id, recruiterId(authentication));
    }

    private Long recruiterId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
