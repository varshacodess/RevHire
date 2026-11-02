package com.revhire.application.service;

import java.util.List;

public interface JobEligibilityVerifier {
    void verifyJobIsOpen(Long jobId, String bearerToken);

    List<OwnedJob> findJobsOwnedByRecruiter(String bearerToken);

    record OwnedJob(Long jobId, String title) {
    }
}
