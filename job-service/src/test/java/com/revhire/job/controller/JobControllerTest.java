package com.revhire.job.controller;

import com.revhire.job.config.SecurityConfig;
import com.revhire.job.dto.CreateJobRequest;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.dto.PageResponse;
import com.revhire.job.dto.UpdateJobRequest;
import com.revhire.job.entity.JobStatus;
import com.revhire.job.exception.ApiExceptionHandler;
import com.revhire.job.exception.JobNotFoundException;
import com.revhire.job.security.JwtAuthenticationFilter;
import com.revhire.job.security.JwtService;
import com.revhire.job.service.JobService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = JobController.class,
        properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
)
@Import({ApiExceptionHandler.class, SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class})
class JobControllerTest {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String WRONG_SECRET = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXphYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService service;

    @Test
    void createsJobAndReturnsLocation() throws Exception {
        when(service.create(any(CreateJobRequest.class)))
                .thenReturn(response(7L, JobStatus.OPEN));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER"))
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
    void existingOpenJobReturnsApplicationEligibility() throws Exception {
        when(service.getById(7L)).thenReturn(response(7L, JobStatus.OPEN));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(7))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.acceptingApplications").value(true))
                .andExpect(jsonPath("$.title").value("Engineer"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.requirements").value("Requirements"))
                .andExpect(jsonPath("$.location").value("Remote"))
                .andExpect(jsonPath("$.employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.experienceYears").value(2))
                .andExpect(jsonPath("$.skills[0]").value("Java"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-28T10:00:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-28T10:00:00"));
    }

    @Test
    void existingClosedJobIsNotEligible() throws Exception {
        when(service.getById(8L)).thenReturn(response(8L, JobStatus.CLOSED));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/8")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.acceptingApplications").value(false));
    }

    @Test
    void combinedSearchCriteriaAreBoundAndReturnedAsPage() throws Exception {
        when(service.search(any(JobSearchRequest.class))).thenReturn(
                new PageResponse<>(List.of(response(7L, JobStatus.OPEN)), 0, 5, 1, 1)
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE"))
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
    void missingJobReturnsNotFoundError() throws Exception {
        when(service.getById(99L))
                .thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/99")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found: 99"));
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        when(service.getById(10L)).thenThrow(new IllegalStateException("SECRET_SQL_DETAILS"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/10")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("SECRET_SQL_DETAILS"))));
    }

    @Test
    void createRejectsInvalidInput() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER"))
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
    void closeEndpointMarksJobAsUnavailable() throws Exception {
        when(service.close(7L)).thenReturn(response(7L, JobStatus.CLOSED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/jobs/7/close")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.acceptingApplications").value(false));
    }

    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearerToken("CANDIDATE", Instant.now().minusSeconds(1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignatureReturnsUnauthorized() throws Exception {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "CANDIDATE")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(WRONG_SECRET)))
                .compact();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotCloseJob() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/jobs/7/close")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE")))
                .andExpect(status().isForbidden());

        verify(service, never()).close(7L);
    }

    @Test
    void candidateCannotCreateOrUpdateJobs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Engineer",
                                  "description": "Description",
                                  "requirements": "Requirements",
                                  "location": "Remote",
                                  "employmentType": "FULL_TIME",
                                  "experienceYears": 2
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any(CreateJobRequest.class));
        verify(service, never()).update(any(Long.class), any(UpdateJobRequest.class));
    }

    @Test
    void recruiterCanUpdateJobs() throws Exception {
        when(service.update(any(Long.class), any(UpdateJobRequest.class)))
                .thenReturn(response(7L, JobStatus.OPEN));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(7));
    }

    @Test
    void recruiterCannotReadCandidateJobDetails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/jobs/7")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("RECRUITER")))
                .andExpect(status().isForbidden());

        verify(service, never()).getById(7L);
    }

    private String bearerToken(String role) {
        return bearerToken(role, Instant.now().plusSeconds(300));
    }

    private String bearerToken(String role, Instant expiration) {
        String token = Jwts.builder()
                .subject("42")
                .claim("email", "test@example.com")
                .claim("role", role)
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
        return "Bearer " + token;
    }

    private JobResponse response(Long id, JobStatus status) {
        return new JobResponse(
                id, true, status, status == JobStatus.OPEN,
                "Engineer", "Description", "Requirements", "Remote",
                "FULL_TIME", 2, Set.of("Java"),
                LocalDateTime.parse("2026-10-28T10:00:00"),
                LocalDateTime.parse("2026-10-28T10:00:00")
        );
    }
}
