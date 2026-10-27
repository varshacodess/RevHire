package com.revhire.application.service;

import org.springframework.security.core.Authentication;

public interface CandidateIdentityProvider {
    Long getCandidateId(Authentication authentication);
}
