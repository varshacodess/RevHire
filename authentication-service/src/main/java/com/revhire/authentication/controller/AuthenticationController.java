package com.revhire.authentication.controller;

import com.revhire.authentication.dto.AuthResponse;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.LoginRequest;
import com.revhire.authentication.dto.RecruiterRegistrationRequest;
import com.revhire.authentication.dto.UserResponse;
import com.revhire.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/candidate/register")
    public ResponseEntity<UserResponse> registerCandidate(@Valid @RequestBody CandidateRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.registerCandidate(request));
    }

    @PostMapping("/recruiter/register")
    public ResponseEntity<UserResponse> registerRecruiter(@Valid @RequestBody RecruiterRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.registerRecruiter(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
