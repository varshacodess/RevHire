package com.revhire.application.controller;

import com.revhire.application.dto.SubmitApplicationRequest;
import com.revhire.application.entity.Application;
import com.revhire.application.exception.DuplicateApplicationException;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.CandidateIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
}
