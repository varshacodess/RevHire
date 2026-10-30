package com.revhire.job.controller;

import com.revhire.job.dto.CreateJobRequest;
import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.PageResponse;
import com.revhire.job.exception.ApiExceptionHandler;
import com.revhire.job.exception.JobNotFoundException;
import com.revhire.job.entity.JobStatus;
import com.revhire.job.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Set;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import(ApiExceptionHandler.class)
class JobControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService service;

    @Test
    @WithMockUser
    void createsJobAndReturnsLocation() throws Exception {
        when(service.create(any(CreateJobRequest.class)))
                .thenReturn(response(7L, JobStatus.OPEN));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/jobs")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Engineer",
                                  "description": "Description",
                                  "requirements": "Requirements",
                                  "location": "Remote",
                                  "employmentType": "FULL_TIME",
                                  "experienceYears": 2,
                                  "skills": ["Java"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Location", "http://localhost/api/jobs/7"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    void existingOpenJobReturnsApplicationEligibility() throws Exception {
        when(service.getById(7L)).thenReturn(response(7L, JobStatus.OPEN));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(7))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.acceptingApplications").value(true));
    }

    @Test
    @WithMockUser
    void existingClosedJobIsNotEligible() throws Exception {
        when(service.getById(8L)).thenReturn(response(8L, JobStatus.CLOSED));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.acceptingApplications").value(false));
    }

    @Test
    @WithMockUser
    void combinedSearchCriteriaAreBoundAndReturnedAsPage() throws Exception {
        when(service.search(any(JobSearchRequest.class))).thenReturn(
                new PageResponse<>(List.of(response(7L, JobStatus.OPEN)), 0, 5, 1, 1)
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs")
                        .param("keywords", "spring")
                        .param("title", "engineer")
                        .param("location", "remote")
                        .param("skill", "Java")
                        .param("employmentType", "FULL_TIME")
                        .param("minExperienceYears", "1")
                        .param("maxExperienceYears", "4")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "TITLE")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].jobId").value(7))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5));

        org.mockito.Mockito.verify(service).search(argThat(criteria ->
                "spring".equals(criteria.getKeywords())
                        && "engineer".equals(criteria.getTitle())
                        && "remote".equals(criteria.getLocation())
                        && "Java".equals(criteria.getSkill())
                        && "FULL_TIME".equals(criteria.getEmploymentType())
                        && criteria.getMinExperienceYears() == 1
                        && criteria.getMaxExperienceYears() == 4
                        && criteria.getSize() == 5
                        && criteria.getSortBy().name().equals("TITLE")
                        && criteria.getDirection().name().equals("ASC")
        ));
    }

    @Test
    @WithMockUser
    void missingJobReturnsNotFoundError() throws Exception {
        when(service.getById(99L))
                .thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found: 99"));
    }

    @Test
    @WithMockUser
    void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        when(service.getById(10L)).thenThrow(new IllegalStateException("SECRET_SQL_DETAILS"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("SECRET_SQL_DETAILS"))));
    }

    @Test
    @WithMockUser
    void createRejectsInvalidInput() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/jobs")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Description",
                                  "requirements": "Requirements",
                                  "location": "Remote",
                                  "employmentType": "FULL_TIME",
                                  "experienceYears": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.experienceYears").exists());
    }

    @Test
    @WithMockUser
    void closeEndpointMarksJobAsUnavailable() throws Exception {
        when(service.close(7L)).thenReturn(response(7L, JobStatus.CLOSED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/jobs/7/close")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.acceptingApplications").value(false));
    }

    private JobResponse response(Long id, JobStatus status) {
        return new JobResponse(
                id, true, status, status == JobStatus.OPEN,
                "Engineer", "Description", "Requirements", "Remote",
                "FULL_TIME", 2, Set.of("Java"), null, null
        );
    }
}
