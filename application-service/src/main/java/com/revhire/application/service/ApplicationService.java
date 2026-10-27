package com.revhire.application.service;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.exception.DuplicateApplicationException;
import com.revhire.application.repository.ApplicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private static final String INITIAL_STATUS = "PENDING";

    private final ApplicationRepository applicationRepository;
    private final JobEligibilityVerifier jobEligibilityVerifier;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            JobEligibilityVerifier jobEligibilityVerifier
    ) {
        this.applicationRepository = applicationRepository;
        this.jobEligibilityVerifier = jobEligibilityVerifier;
    }

    @Transactional
    public Application submit(Long candidateId, SubmitApplicationRequest request) {
        jobEligibilityVerifier.verifyJobIsOpen(request.jobId());

        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, request.jobId())) {
            throw new DuplicateApplicationException(
                    "Candidate has already applied for this job"
            );
        }

        try {
            return applicationRepository.save(
                    new Application(candidateId, request.jobId(), INITIAL_STATUS)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateApplicationException(
                    "Candidate has already applied for this job",
                    exception
            );
        }
    }
}
