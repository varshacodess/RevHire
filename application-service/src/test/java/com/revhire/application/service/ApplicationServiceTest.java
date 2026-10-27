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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(applicationRepository.save(org.mockito.ArgumentMatchers.any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.submit(41L, new SubmitApplicationRequest(12L));

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(jobEligibilityVerifier).verifyJobIsOpen(12L);
        verify(applicationRepository).save(applicationCaptor.capture());
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
    }
}
