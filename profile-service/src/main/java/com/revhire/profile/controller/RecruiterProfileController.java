package com.revhire.profile.controller;

import com.revhire.profile.dto.RecruiterProfileRequest;
import com.revhire.profile.dto.RecruiterProfileResponse;
import com.revhire.profile.service.RecruiterProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruiters/profile")
public class RecruiterProfileController {
    private final RecruiterProfileService profileService;

    public RecruiterProfileController(RecruiterProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<RecruiterProfileResponse> create(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.create(Long.valueOf(authentication.getName()), request));
    }

    @GetMapping
    public RecruiterProfileResponse get(Authentication authentication) {
        return profileService.get(Long.valueOf(authentication.getName()));
    }

    @PutMapping
    public RecruiterProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody RecruiterProfileRequest request
    ) {
        return profileService.update(Long.valueOf(authentication.getName()), request);
    }
}
