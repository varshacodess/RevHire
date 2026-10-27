package com.revhire.application.service;

import com.revhire.application.exception.DependencyUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class UnavailableJobEligibilityVerifier implements JobEligibilityVerifier {

    @Override
    public void verifyJobIsOpen(Long jobId) {
        throw new DependencyUnavailableException(
                "Job Service eligibility verification is not configured"
        );
    }
}
