package com.revhire.authentication.controller;

import com.revhire.authentication.dto.CandidateLoginRequest;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.CandidateResponse;
import com.revhire.authentication.service.CandidateAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/candidates")
public class CandidateAuthController {
    private final CandidateAuthService candidateAuthService;

    public CandidateAuthController(CandidateAuthService candidateAuthService) {
        this.candidateAuthService = candidateAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<CandidateResponse> register(
            @Valid @RequestBody CandidateRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateAuthService.register(request));
    }

    @PostMapping("/login")
    public CandidateResponse login(@Valid @RequestBody CandidateLoginRequest request) {
        return candidateAuthService.login(request);
    }
}
