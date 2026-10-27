package com.revhire.application.service;

import com.revhire.application.exception.AuthenticationRequiredException;
import com.revhire.application.exception.DependencyUnavailableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCandidateIdentityProvider implements CandidateIdentityProvider {

    @Override
    public Long getCandidateId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException("Authentication is required");
        }

        if (!(authentication.getPrincipal() instanceof CandidatePrincipal candidatePrincipal)) {
            throw new DependencyUnavailableException(
                    "Authentication does not expose a candidate identity; configure the authentication integration"
            );
        }

        Long candidateId = candidatePrincipal.getCandidateId();
        if (candidateId == null || candidateId <= 0) {
            throw new DependencyUnavailableException(
                    "Authentication does not provide a valid candidate ID"
            );
        }
        return candidateId;
    }
}
