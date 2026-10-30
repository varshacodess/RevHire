package com.revhire.application.service;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

        Application result = applicationService.submit(41L, new SubmitApplicationRequest(12L));

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(jobEligibilityVerifier).verifyJobIsOpen(12L);
        verify(applicationRepository).saveAndFlush(applicationCaptor.capture());
        assertEquals(41L, result.getCandidateId());
        assertEquals(12L, result.getJobId());
        assertEquals("PENDING", applicationCaptor.getValue().getStatus());
    }

    @Test
    void rejectsApplicationWhenCandidateAlreadyApplied() {
        when(applicationRepository.existsByCandidateIdAndJobId(41L, 12L)).thenReturn(true);

        assertThrows(
                DuplicateApplicationException.class,
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L))
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
                () -> applicationService.submit(41L, new SubmitApplicationRequest(12L))
        );

        assertEquals("Candidate has already applied for this job", exception.getMessage());
        assertEquals(databaseException, exception.getCause());
        verify(applicationRepository).saveAndFlush(any(Application.class));
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
        when(applicationRepository.findAllByCandidateIdAndStatus(41L, "REVIEWING", pageable))
                .thenReturn(expected);

        Page<Application> result = applicationService.findByCandidateId(41L, "REVIEWING", pageable);

        assertEquals(expected, result);
        verify(applicationRepository).findAllByCandidateIdAndStatus(41L, "REVIEWING", pageable);
    }

    @Test
    void returnsEmptyPageWhenCandidateHasNoMatchingApplications() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate"));
        when(applicationRepository.findAllByCandidateIdAndStatus(41L, "REVIEWING", pageable))
                .thenReturn(Page.empty(pageable));

        Page<Application> result = applicationService.findByCandidateId(41L, "REVIEWING", pageable);

        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        verify(applicationRepository).findAllByCandidateIdAndStatus(41L, "REVIEWING", pageable);
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

    private Application application(
            Long candidateId,
            Long jobId,
            Long applicationId,
            LocalDateTime appliedDate
    ) {
        Application application = new Application(candidateId, jobId, "PENDING");
        ReflectionTestUtils.setField(application, "applicationId", applicationId);
        ReflectionTestUtils.setField(application, "appliedDate", appliedDate);
        ReflectionTestUtils.setField(application, "updatedDate", appliedDate.plusHours(1));
        return application;
    }
}
