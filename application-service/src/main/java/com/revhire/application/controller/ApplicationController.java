package com.revhire.application.controller;

import com.revhire.application.dto.ApplicationResponse;
import com.revhire.application.dto.RecruiterApplicationResponse;
import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.dto.UpdateApplicationStatusRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;
import com.revhire.application.exception.AuthenticationRequiredException;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.CandidateIdentityProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;

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
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AuthenticationRequiredException("A candidate JWT identity is required");
        }
        Application application = applicationService.submit(
                candidateId,
                request,
                jwtAuthentication.getToken().getTokenValue()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{applicationId}")
                .buildAndExpand(application.getApplicationId())
                .toUri();
        return ResponseEntity.created(location).body(ApplicationResponse.from(application));
    }

    @GetMapping("/recruiter/me")
    public ResponseEntity<RecruiterApplicationPageResponse> findRecruiterApplications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePage(page, size);
        String bearerToken = requireRecruiterToken(authentication);
        Page<RecruiterApplicationResponse> applicationPage = applicationService.findByRecruiter(
                bearerToken,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedDate"))
        );
        return ResponseEntity.ok(new RecruiterApplicationPageResponse(
                applicationPage.getContent(),
                applicationPage.getTotalElements(),
                applicationPage.getTotalPages(),
                applicationPage.getNumber(),
                applicationPage.getSize()
        ));
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<RecruiterApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            Authentication authentication,
            @Valid @RequestBody UpdateApplicationStatusRequest request
    ) {
        if (applicationId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicationId must be positive");
        }

        String bearerToken = requireRecruiterToken(authentication);
        return applicationService.updateStatusForRecruiter(
                        applicationId,
                        request.status(),
                        bearerToken
                )
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<List<ApplicationResponse>> findMyApplications(Authentication authentication) {
        Long candidateId = candidateIdentityProvider.getCandidateId(authentication);
        List<ApplicationResponse> applications = applicationService.findByCandidateId(candidateId)
                .stream()
                .map(ApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/me/summary")
    public ResponseEntity<ApplicationService.ApplicationSummary> getMyApplicationSummary(
            Authentication authentication
    ) {
        Long candidateId = candidateIdentityProvider.getCandidateId(authentication);
        return ResponseEntity.ok(applicationService.getSummaryByCandidateId(candidateId));
    }

    @GetMapping("/me/page")
    public ResponseEntity<ApplicationPageResponse> findMyApplicationsPage(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        validatePage(page, size);

        ApplicationStatus applicationStatus = null;
        if (status != null) {
            try {
                applicationStatus = ApplicationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "status must be PENDING, REVIEWING, ACCEPTED or REJECTED",
                        exception
                );
            }
        }

        Long candidateId = candidateIdentityProvider.getCandidateId(authentication);
        Page<ApplicationResponse> applicationPage = applicationService.findByCandidateId(
                        candidateId,
                        applicationStatus,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedDate"))
                )
                .map(ApplicationResponse::from);

        return ResponseEntity.ok(new ApplicationPageResponse(
                applicationPage.getContent(),
                applicationPage.getTotalElements(),
                applicationPage.getTotalPages(),
                applicationPage.getNumber(),
                applicationPage.getSize()
        ));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> findApplication(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        if (applicationId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "applicationId must be positive");
        }

        Long candidateId = candidateIdentityProvider.getCandidateId(authentication);
        return applicationService.findByApplicationIdAndCandidateId(applicationId, candidateId)
                .map(ApplicationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ApplicationPageResponse(
            List<ApplicationResponse> applications,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize
    ) {
    }

    public record RecruiterApplicationPageResponse(
            List<RecruiterApplicationResponse> applications,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize
    ) {
    }

    private String requireRecruiterToken(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RECRUITER"))
                || jwtAuthentication.getToken().getSubject() == null
                || jwtAuthentication.getToken().getSubject().isBlank()) {
            throw new AuthenticationRequiredException("A recruiter JWT identity is required");
        }
        return jwtAuthentication.getToken().getTokenValue();
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
