package com.revhire.application.service;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.dto.RecruiterApplicationResponse;
import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;
import com.revhire.application.exception.DuplicateApplicationException;
import com.revhire.application.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobEligibilityVerifier jobEligibilityVerifier;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void savesApplicationWithInitialStatusAfterEligibilityCheck() {
        when(applicationRepository.existsByCandidateIdAndJobId(41L, 12L)).thenReturn(false);
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.submit(
                41L,
                new SubmitApplicationRequest(12L),
                "candidate-test-token"
        );

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(jobEligibilityVerifier).verifyJobIsOpen(12L, "candidate-test-token");
        verify(applicationRepository).saveAndFlush(applicationCaptor.capture());
        assertEquals(41L, result.getCandidateId());
        assertEquals(12L, result.getJobId());
        assertEquals(ApplicationStatus.PENDING, applicationCaptor.getValue().getStatus());
    }

    @Test
    void rejectsApplicationWhenCandidateAlreadyApplied() {
        when(applicationRepository.existsByCandidateIdAndJobId(41L, 12L)).thenReturn(true);

        assertThrows(
                DuplicateApplicationException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L), "candidate-test-token")
        );
        verify(applicationRepository, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void convertsDatabaseConstraintViolationToDuplicateApplicationException() {
        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException("Unique constraint violation");
        when(applicationRepository.existsByCandidateIdAndJobId(41L, 12L)).thenReturn(false);
        when(applicationRepository.saveAndFlush(any(Application.class))).thenThrow(databaseException);

        DuplicateApplicationException exception = assertThrows(
                DuplicateApplicationException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L), "candidate-test-token")
        );

        assertEquals("Candidate has already applied for this job", exception.getMessage());
        assertEquals(databaseException, exception.getCause());
        verify(applicationRepository).saveAndFlush(any(Application.class));
    }

    @Test
    void doesNotSaveWhenJobIsClosed() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Job is not accepting applications"))
                .when(jobEligibilityVerifier).verifyJobIsOpen(12L, "candidate-test-token");

        assertThrows(
                IllegalArgumentException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L), "candidate-test-token")
        );

        verify(applicationRepository, never()).existsByCandidateIdAndJobId(41L, 12L);
        verify(applicationRepository, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void doesNotSaveWhenJobDoesNotExist() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Job does not exist"))
                .when(jobEligibilityVerifier).verifyJobIsOpen(12L, "candidate-test-token");

        assertThrows(
                IllegalArgumentException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L), "candidate-test-token")
        );

        verify(applicationRepository, never()).existsByCandidateIdAndJobId(41L, 12L);
        verify(applicationRepository, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void doesNotSaveWhenJobServiceIsUnavailable() {
        org.mockito.Mockito.doThrow(new com.revhire.application.exception.DependencyUnavailableException(
                        "Job Service is unavailable"
                ))
                .when(jobEligibilityVerifier).verifyJobIsOpen(12L, "candidate-test-token");

        assertThrows(
                com.revhire.application.exception.DependencyUnavailableException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L), "candidate-test-token")
        );

        verify(applicationRepository, never()).existsByCandidateIdAndJobId(41L, 12L);
        verify(applicationRepository, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void findsOnlyCandidateApplicationsInNewestFirstOrder() {
        Application newest = application(41L, 12L, 2L, LocalDateTime.parse("2026-10-29T12:00:00"));
        Application older = application(41L, 14L, 1L, LocalDateTime.parse("2026-10-28T12:00:00"));
        Application anotherCandidatesApplication = application(
                42L, 15L, 3L, LocalDateTime.parse("2026-10-30T12:00:00")
        );
        when(applicationRepository.findAllByCandidateIdOrderByAppliedDateDesc(41L))
                .thenReturn(List.of(newest, older, anotherCandidatesApplication));

        List<Application> result = applicationService.findByCandidateId(41L);

        assertEquals(List.of(newest, older), result);
        verify(applicationRepository).findAllByCandidateIdOrderByAppliedDateDesc(41L);
    }

    @Test
    void returnsEmptyListWhenCandidateHasNoApplications() {
        when(applicationRepository.findAllByCandidateIdOrderByAppliedDateDesc(41L))
                .thenReturn(List.of());

        assertEquals(List.of(), applicationService.findByCandidateId(41L));
    }

    @Test
    void findsCandidateApplicationsWithPaginationAndNewestFirstSort() {
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "appliedDate"));
        List<Application> applications = List.of(
                application(41L, 12L, 7L, LocalDateTime.parse("2026-10-29T12:00:00")),
                application(41L, 14L, 8L, LocalDateTime.parse("2026-10-28T12:00:00"))
        );
        Page<Application> expected = new PageImpl<>(applications, pageable, 5);
        when(applicationRepository.findAllByCandidateId(41L, pageable)).thenReturn(expected);

        Page<Application> result = applicationService.findByCandidateId(41L, null, pageable);

        assertEquals(expected, result);
        verify(applicationRepository).findAllByCandidateId(41L, pageable);
    }

    @Test
    void filtersPaginatedApplicationsByStatusAndCandidate() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate"));
        Page<Application> expected = new PageImpl<>(
                List.of(application(41L, 12L, 7L, LocalDateTime.parse("2026-10-29T12:00:00")))
        );
        when(applicationRepository.findAllByCandidateIdAndStatus(41L, ApplicationStatus.REVIEWING, pageable))
                .thenReturn(expected);

        Page<Application> result = applicationService.findByCandidateId(41L, ApplicationStatus.REVIEWING, pageable);

        assertEquals(expected, result);
        verify(applicationRepository).findAllByCandidateIdAndStatus(41L, ApplicationStatus.REVIEWING, pageable);
    }

    @Test
    void returnsEmptyPageWhenCandidateHasNoMatchingApplications() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate"));
        when(applicationRepository.findAllByCandidateIdAndStatus(41L, ApplicationStatus.REVIEWING, pageable))
                .thenReturn(Page.empty(pageable));

        Page<Application> result = applicationService.findByCandidateId(41L, ApplicationStatus.REVIEWING, pageable);

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        verify(applicationRepository).findAllByCandidateIdAndStatus(41L, ApplicationStatus.REVIEWING, pageable);
    }

    @Test
    void totalSummaryIncludesApplicationsWithOtherStatuses() {
        ApplicationRepository.ApplicationSummaryCounts counts =
                mock(ApplicationRepository.ApplicationSummaryCounts.class);
        when(counts.getTotalApplications()).thenReturn(4L);
        when(counts.getPendingApplications()).thenReturn(2L);
        when(counts.getReviewingApplications()).thenReturn(1L);
        when(applicationRepository.getSummaryByCandidateId(41L)).thenReturn(counts);

        ApplicationService.ApplicationSummary summary = applicationService.getSummaryByCandidateId(41L);

        assertEquals(new ApplicationService.ApplicationSummary(4, 2, 1), summary);
        verify(applicationRepository).getSummaryByCandidateId(41L);
    }

    @Test
    void returnsZeroCountsForCandidateWithNoApplications() {
        ApplicationRepository.ApplicationSummaryCounts counts =
                mock(ApplicationRepository.ApplicationSummaryCounts.class);
        when(counts.getTotalApplications()).thenReturn(0L);
        when(counts.getPendingApplications()).thenReturn(0L);
        when(counts.getReviewingApplications()).thenReturn(0L);
        when(applicationRepository.getSummaryByCandidateId(42L)).thenReturn(counts);

        ApplicationService.ApplicationSummary summary = applicationService.getSummaryByCandidateId(42L);

        assertEquals(new ApplicationService.ApplicationSummary(0, 0, 0), summary);
        verify(applicationRepository).getSummaryByCandidateId(42L);
    }

    @Test
    void findsApplicationOnlyForItsCandidate() {
        Application application = application(41L, 12L, 7L, LocalDateTime.parse("2026-10-29T12:00:00"));
        when(applicationRepository.findByApplicationIdAndCandidateId(7L, 41L))
                .thenReturn(Optional.of(application));
        when(applicationRepository.findByApplicationIdAndCandidateId(7L, 42L))
                .thenReturn(Optional.empty());

        assertEquals(Optional.of(application), applicationService.findByApplicationIdAndCandidateId(7L, 41L));
        assertEquals(Optional.empty(), applicationService.findByApplicationIdAndCandidateId(7L, 42L));
        verify(applicationRepository).findByApplicationIdAndCandidateId(7L, 41L);
        verify(applicationRepository).findByApplicationIdAndCandidateId(7L, 42L);
    }

    @Test
    void returnsEmptyWhenApplicationDoesNotExist() {
        when(applicationRepository.findByApplicationIdAndCandidateId(99L, 41L))
                .thenReturn(Optional.empty());

        assertEquals(Optional.empty(), applicationService.findByApplicationIdAndCandidateId(99L, 41L));
    }

    @Test
    void recruiterListsOnlyApplicationsForJobsReturnedAsOwnedByJobService() {
        JobEligibilityVerifier.OwnedJob ownedJob = new JobEligibilityVerifier.OwnedJob(12L, "Backend Engineer");
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate"));
        Application application = application(
                41L, 12L, 7L, LocalDateTime.parse("2026-10-29T12:00:00")
        );
        when(jobEligibilityVerifier.findJobsOwnedByRecruiter("recruiter-token"))
                .thenReturn(List.of(ownedJob));
        when(applicationRepository.findAllByJobIdIn(Set.of(12L), pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        Page<RecruiterApplicationResponse> result = applicationService.findByRecruiter(
                "recruiter-token",
                pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(12L, result.getContent().getFirst().jobId());
        assertEquals("Backend Engineer", result.getContent().getFirst().jobTitle());
        verify(applicationRepository).findAllByJobIdIn(Set.of(12L), pageable);
    }

    @Test
    void recruiterCannotUpdateApplicationForJobNotOwnedByJobService() {
        Application foreignApplication = application(
                42L, 99L, 8L, LocalDateTime.parse("2026-10-29T12:00:00")
        );
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(foreignApplication));
        when(jobEligibilityVerifier.findJobsOwnedByRecruiter("recruiter-token"))
                .thenReturn(List.of(new JobEligibilityVerifier.OwnedJob(12L, "Owned job")));

        assertEquals(Optional.empty(), applicationService.updateStatusForRecruiter(
                8L,
                ApplicationStatus.REJECTED,
                "recruiter-token"
        ));

        verify(applicationRepository, never()).saveAndFlush(any(Application.class));
    }

    @Test
    void recruiterCanUpdateStatusForOwnedApplication() {
        Application application = application(
                41L, 12L, 7L, LocalDateTime.parse("2026-10-29T12:00:00")
        );
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));
        when(jobEligibilityVerifier.findJobsOwnedByRecruiter("recruiter-token"))
                .thenReturn(List.of(new JobEligibilityVerifier.OwnedJob(12L, "Backend Engineer")));
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        RecruiterApplicationResponse result = applicationService.updateStatusForRecruiter(
                7L,
                ApplicationStatus.ACCEPTED,
                "recruiter-token"
        ).orElseThrow();

        assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
        assertEquals(ApplicationStatus.ACCEPTED, result.applicationStatus());
        assertEquals("Backend Engineer", result.jobTitle());
        assertEquals(application.getUpdatedDate(), result.updatedDate());
        verify(applicationRepository).saveAndFlush(application);
    }

    @Test
    void recruiterListingFailsClosedWhenJobServiceIsUnavailable() {
        org.mockito.Mockito.doThrow(new com.revhire.application.exception.DependencyUnavailableException(
                        "Job Service is unavailable"
                ))
                .when(jobEligibilityVerifier).findJobsOwnedByRecruiter("recruiter-token");

        assertThrows(
                com.revhire.application.exception.DependencyUnavailableException.class,
                () -> applicationService.findByRecruiter(
                        "recruiter-token",
                        PageRequest.of(0, 10)
                )
        );

        verify(applicationRepository, never()).findAllByJobIdIn(any(), any());
    }

    private Application application(
            Long candidateId,
            Long jobId,
            Long applicationId,
            LocalDateTime appliedDate
    ) {
        Application application = new Application(candidateId, jobId, ApplicationStatus.PENDING);
        ReflectionTestUtils.setField(application, "applicationId", applicationId);
        ReflectionTestUtils.setField(application, "appliedDate", appliedDate);
        ReflectionTestUtils.setField(application, "updatedDate", appliedDate.plusHours(1));
        return application;
    }
}
