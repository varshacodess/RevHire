package com.revhire.application.service;

import com.revhire.application.exception.AuthenticationRequiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCandidateIdentityProvider implements CandidateIdentityProvider {

    @Override
    public Long getCandidateId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException("Authentication is required");
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CANDIDATE"))) {
            throw new AuthenticationRequiredException("A candidate JWT identity is required");
        }

        String subject = jwtAuthentication.getToken().getSubject();
        try {
            long candidateId = Long.parseLong(subject);
            if (candidateId <= 0) {
                throw new NumberFormatException("Candidate ID must be positive");
            }
            return candidateId;
        } catch (NumberFormatException exception) {
            throw new AuthenticationRequiredException("JWT subject must be a positive candidate ID");
        }
    }
}
