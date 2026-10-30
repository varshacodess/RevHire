package com.revhire.application.controller;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.exception.AuthenticationRequiredException;
import com.revhire.application.exception.DependencyUnavailableException;
import com.revhire.application.exception.DuplicateApplicationException;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.CandidateIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private CandidateIdentityProvider candidateIdentityProvider;

    @Test
    void submitsApplicationAndReturnsCreatedResponse() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        Application saved = new Application(41L, 12L, "PENDING");
        ReflectionTestUtils.setField(saved, "applicationId", 7L);
        when(applicationService.submit(eq(41L), any(SubmitApplicationRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/applications")
                        .with(user("candidate"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":12}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/applications/7"))
                .andExpect(jsonPath("$.applicationId").value(7))
                .andExpect(jsonPath("$.candidateId").value(41))
                .andExpect(jsonPath("$.jobId").value(12))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(candidateIdentityProvider).getCandidateId(any());
    }

    @Test
    void rejectsMissingJobId() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(user("candidate"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.jobId").value("jobId is required"));
    }

    @Test
    void returnsConflictForDuplicateApplication() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        when(applicationService.submit(eq(41L), any(SubmitApplicationRequest.class)))
                .thenThrow(new DuplicateApplicationException("Candidate has already applied for this job"));

        mockMvc.perform(post("/api/applications")
                        .with(user("candidate"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobId\":12}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Candidate has already applied for this job"));
    }

    @Test
    void returnsCurrentCandidatesApplicationsWithExistingResponseFields() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        Application newest = application(
                41L, 12L, 7L, "PENDING", LocalDateTime.parse("2026-10-29T12:00:00")
        );
        Application older = application(
                41L, 14L, 8L, "REVIEWING", LocalDateTime.parse("2026-10-28T12:00:00")
        );
        when(applicationService.findByCandidateId(41L)).thenReturn(List.of(newest, older));

        mockMvc.perform(get("/api/applications/me?candidateId=42").with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value(7))
                .andExpect(jsonPath("$[0].candidateId").value(41))
                .andExpect(jsonPath("$[0].jobId").value(12))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].appliedDate").value("2026-10-29T12:00:00"))
                .andExpect(jsonPath("$[0].updatedDate").value("2026-10-29T13:00:00"))
                .andExpect(jsonPath("$[1].applicationId").value(8))
                .andExpect(jsonPath("$[1].jobId").value(14));

        verify(candidateIdentityProvider).getCandidateId(any());
        verify(applicationService).findByCandidateId(41L);
    }

    @Test
    void returnsEmptyArrayWhenCandidateHasNoApplications() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        when(applicationService.findByCandidateId(41L)).thenReturn(List.of());

        mockMvc.perform(get("/api/applications/me").with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returnsPaginatedApplicationsForCurrentCandidate() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        List<Application> applications = List.of(
                application(41L, 12L, 7L, "PENDING", LocalDateTime.parse("2026-10-29T12:00:00")),
                application(41L, 14L, 8L, "REVIEWING", LocalDateTime.parse("2026-10-28T12:00:00"))
        );
        when(applicationService.findByCandidateId(
                eq(41L),
                org.mockito.ArgumentMatchers.isNull(),
                eq(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "appliedDate")))
        )).thenReturn(new PageImpl<>(
                applications,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "appliedDate")),
                5
        ));

        mockMvc.perform(get("/api/applications/me/page?page=1&size=2&candidateId=99")
                        .with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.length()").value(2))
                .andExpect(jsonPath("$.applications[0].applicationId").value(7))
                .andExpect(jsonPath("$.applications[1].applicationId").value(8))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(2));

        verify(candidateIdentityProvider).getCandidateId(any());
        verify(applicationService).findByCandidateId(
                eq(41L),
                org.mockito.ArgumentMatchers.isNull(),
                eq(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "appliedDate")))
        );
    }

    @Test
    void filtersPaginatedApplicationsByStatus() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        Application application = application(
                41L, 12L, 7L, "REVIEWING", LocalDateTime.parse("2026-10-29T12:00:00")
        );
        when(applicationService.findByCandidateId(
                eq(41L),
                eq("REVIEWING"),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate")))
        )).thenReturn(new PageImpl<>(
                List.of(application),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate")),
                1
        ));

        mockMvc.perform(get("/api/applications/me/page?status=reviewing").with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.length()").value(1))
                .andExpect(jsonPath("$.applications[0].status").value("REVIEWING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

        verify(applicationService).findByCandidateId(
                eq(41L),
                eq("REVIEWING"),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate")))
        );
    }

    @Test
    void returnsEmptyPaginatedResults() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        when(applicationService.findByCandidateId(
                eq(41L),
                eq("REVIEWING"),
                any()
        )).thenReturn(Page.empty(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "appliedDate"))
        ));

        mockMvc.perform(get("/api/applications/me/page?status=REVIEWING").with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications").isArray())
                .andExpect(jsonPath("$.applications").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/applications/me/page?page=-1").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.parameter").value("page must be non-negative"));
    }

    @Test
    void rejectsSizeOutsideAllowedRange() throws Exception {
        mockMvc.perform(get("/api/applications/me/page?size=101").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.parameter").value("size must be between 1 and 100"));
        mockMvc.perform(get("/api/applications/me/page?size=0").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.parameter").value("size must be between 1 and 100"));
    }

    @Test
    void rejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/applications/me/page?status=UNKNOWN").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.parameter").value("status must be PENDING or REVIEWING"));
    }

    @Test
    void rejectsMalformedPageAndSizeValues() throws Exception {
        mockMvc.perform(get("/api/applications/me/page?page=abc").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.page").exists());
        mockMvc.perform(get("/api/applications/me/page?size=abc").with(user("candidate")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.size").exists());
    }

    @Test
    void returnsUnauthorizedWhenCandidateIdentityIsMissing() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any()))
                .thenThrow(new AuthenticationRequiredException("Authentication is required"));

        mockMvc.perform(get("/api/applications/me").with(user("candidate")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void returnsServiceUnavailableWhenCandidateIdentityCannotBeResolved() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any()))
                .thenThrow(new DependencyUnavailableException("Authentication does not expose a candidate identity"));

        mockMvc.perform(get("/api/applications/me").with(user("candidate")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Authentication does not expose a candidate identity"));
    }

    @Test
    void returnsApplicationWhenItBelongsToCurrentCandidate() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        Application application = application(
                41L, 12L, 7L, "PENDING", LocalDateTime.parse("2026-10-29T12:00:00")
        );
        when(applicationService.findByApplicationIdAndCandidateId(7L, 41L))
                .thenReturn(Optional.of(application));

        mockMvc.perform(get("/api/applications/7").with(user("candidate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(7))
                .andExpect(jsonPath("$.candidateId").value(41))
                .andExpect(jsonPath("$.jobId").value(12))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.appliedDate").value("2026-10-29T12:00:00"))
                .andExpect(jsonPath("$.updatedDate").value("2026-10-29T13:00:00"));

        verify(applicationService).findByApplicationIdAndCandidateId(7L, 41L);
    }

    @Test
    void returnsNotFoundWhenApplicationDoesNotExist() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        when(applicationService.findByApplicationIdAndCandidateId(99L, 41L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/applications/99").with(user("candidate")))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenApplicationBelongsToAnotherCandidate() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any())).thenReturn(41L);
        when(applicationService.findByApplicationIdAndCandidateId(7L, 41L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/applications/7").with(user("candidate")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsNonPositiveApplicationId() throws Exception {
        mockMvc.perform(get("/api/applications/0").with(user("candidate")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonNumericApplicationId() throws Exception {
        mockMvc.perform(get("/api/applications/not-a-number").with(user("candidate")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsUnauthorizedWhenIdentityIsMissingForSingleApplication() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any()))
                .thenThrow(new AuthenticationRequiredException("Authentication is required"));

        mockMvc.perform(get("/api/applications/7").with(user("candidate")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void returnsServiceUnavailableWhenIdentityCannotBeResolvedForSingleApplication() throws Exception {
        when(candidateIdentityProvider.getCandidateId(any()))
                .thenThrow(new DependencyUnavailableException("Authentication does not expose a candidate identity"));

        mockMvc.perform(get("/api/applications/7").with(user("candidate")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Authentication does not expose a candidate identity"));
    }

    private Application application(
            Long candidateId,
            Long jobId,
            Long applicationId,
            String status,
            LocalDateTime appliedDate
    ) {
        Application application = new Application(candidateId, jobId, status);
        ReflectionTestUtils.setField(application, "applicationId", applicationId);
        ReflectionTestUtils.setField(application, "appliedDate", appliedDate);
        ReflectionTestUtils.setField(application, "updatedDate", appliedDate.plusHours(1));
        return application;
    }
}
