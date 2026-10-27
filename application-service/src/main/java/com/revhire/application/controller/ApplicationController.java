package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.CandidateIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CandidateIdentityProvider candidateIdentityProvider;

    public ApplicationController(
            ApplicationService applicationService,
            CandidateIdentityProvider candidateIdentityProvider
    ) {
        this.applicationService = applicationService;
        this.candidateIdentityProvider = candidateIdentityProvider;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitApplicationRequest request
    ) {
        Long candidateId = candidateIdentityProvider.getCandidateId(authentication);
        Application application = applicationService.submit(candidateId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{applicationId}")
                .buildAndExpand(application.getApplicationId())
                .toUri();
        return ResponseEntity.created(location).body(ApplicationResponse.from(application));
    }
}
