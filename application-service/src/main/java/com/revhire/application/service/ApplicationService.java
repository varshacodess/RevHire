package com.revhire.application.service;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;
import com.revhire.application.exception.DuplicateApplicationException;
import com.revhire.application.repository.ApplicationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ApplicationService {

    private static final ApplicationStatus INITIAL_STATUS = ApplicationStatus.PENDING;

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
            return applicationRepository.saveAndFlush(
                    new Application(candidateId, request.jobId(), INITIAL_STATUS)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateApplicationException(
                    "Candidate has already applied for this job",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Application> findByCandidateId(Long candidateId) {
        return applicationRepository
                .findAllByCandidateIdOrderByAppliedDateDesc(candidateId)
                .stream()
                .filter(application -> Objects.equals(candidateId, application.getCandidateId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<Application> findByCandidateId(
            Long candidateId,
            ApplicationStatus status,
            Pageable pageable
    ) {
        if (status == null) {
            return applicationRepository.findAllByCandidateId(candidateId, pageable);
        }
        return applicationRepository.findAllByCandidateIdAndStatus(candidateId, status, pageable);
    }

    @Transactional(readOnly = true)
    public ApplicationSummary getSummaryByCandidateId(Long candidateId) {
        ApplicationRepository.ApplicationSummaryCounts counts =
                applicationRepository.getSummaryByCandidateId(candidateId);
        return new ApplicationSummary(
                counts.getTotalApplications(),
                counts.getPendingApplications(),
                counts.getReviewingApplications()
        );
    }

    @Transactional(readOnly = true)
    public Optional<Application> findByApplicationIdAndCandidateId(Long applicationId, Long candidateId) {
        return applicationRepository.findByApplicationIdAndCandidateId(applicationId, candidateId);
    }

    public record ApplicationSummary(
            long totalApplications,
            long pendingApplications,
            long reviewingApplications
    ) {
    }
}
