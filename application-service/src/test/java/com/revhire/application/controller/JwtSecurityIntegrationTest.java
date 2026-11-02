package com.revhire.application.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.revhire.application.config.JwtSecurityConfiguration;
import com.revhire.application.dto.RecruiterApplicationResponse;
import com.revhire.application.entity.ApplicationStatus;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.SecurityContextCandidateIdentityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.security.SecureRandom;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import({JwtSecurityConfiguration.class, SecurityContextCandidateIdentityProvider.class})
class JwtSecurityIntegrationTest {

    private static final byte[] TEST_SECRET = new byte[64];

    static {
        new java.security.SecureRandom().nextBytes(TEST_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> Base64.getEncoder().encodeToString(TEST_SECRET));
    }

    @Test
    void acceptsValidCandidateJwtAndExtractsCandidateIdFromSubject() throws Exception {
        when(applicationService.getSummaryByCandidateId(41L))
                .thenReturn(new ApplicationService.ApplicationSummary(2, 1, 1));

        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "CANDIDATE", Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isOk());

        verify(applicationService).getSummaryByCandidateId(41L);
    }

    @Test
    void forbidsValidRecruiterJwtFromCandidateEndpoints() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "RECRUITER", Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recruiterCanRetrieveApplicationsFromRecruiterEndpoint() throws Exception {
        String tokenHeader = bearerToken("recruiter-41", "RECRUITER", Instant.now().plusSeconds(3600),
                JWSAlgorithm.HS512, TEST_SECRET);
        String tokenValue = tokenHeader.substring("Bearer ".length());
        when(applicationService.findByRecruiter(
                org.mockito.ArgumentMatchers.eq(tokenValue),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PageImpl<>(
                java.util.List.of(new RecruiterApplicationResponse(
                        7L,
                        12L,
                        "Backend Engineer",
                        41L,
                        ApplicationStatus.PENDING,
                        null,
                        null
                )),
                PageRequest.of(0, 10),
                1
        ));
        mockMvc.perform(get("/api/applications/recruiter/me")
                        .header("Authorization", tokenHeader))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applications[0].applicationId").value(7))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applications[0].jobTitle").value("Backend Engineer"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applications[0].candidateId").value(41))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applications[0].applicationStatus").value("PENDING"));

        verify(applicationService).findByRecruiter(
                org.mockito.ArgumentMatchers.eq(tokenValue),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void recruiterCanUpdateApplicationStatus() throws Exception {
        String tokenHeader = bearerToken("recruiter-41", "RECRUITER", Instant.now().plusSeconds(3600),
                JWSAlgorithm.HS512, TEST_SECRET);
        String tokenValue = tokenHeader.substring("Bearer ".length());
        when(applicationService.updateStatusForRecruiter(7L, ApplicationStatus.REVIEWING, tokenValue))
                .thenReturn(java.util.Optional.of(new RecruiterApplicationResponse(
                        7L,
                        12L,
                        "Backend Engineer",
                        41L,
                        ApplicationStatus.REVIEWING,
                        null,
                        null
                )));
        mockMvc.perform(patch("/api/applications/7/status")
                        .header("Authorization", tokenHeader)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWING\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applicationStatus").value("REVIEWING"));

        verify(applicationService).updateStatusForRecruiter(7L, ApplicationStatus.REVIEWING, tokenValue);
    }

    @Test
    void recruiterCanRejectApplication() throws Exception {
        String tokenHeader = bearerToken("recruiter-41", "RECRUITER", Instant.now().plusSeconds(3600),
                JWSAlgorithm.HS512, TEST_SECRET);
        String tokenValue = tokenHeader.substring("Bearer ".length());
        when(applicationService.updateStatusForRecruiter(7L, ApplicationStatus.REJECTED, tokenValue))
                .thenReturn(java.util.Optional.of(new RecruiterApplicationResponse(
                        7L,
                        12L,
                        "Backend Engineer",
                        41L,
                        ApplicationStatus.REJECTED,
                        null,
                        null
                )));

        mockMvc.perform(patch("/api/applications/7/status")
                        .header("Authorization", tokenHeader)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.applicationStatus").value("REJECTED"));

        verify(applicationService).updateStatusForRecruiter(7L, ApplicationStatus.REJECTED, tokenValue);
    }

    @Test
    void candidateCannotAccessRecruiterListingOrChangeApplicationStatus() throws Exception {
        String candidateToken = bearerToken("41", "CANDIDATE", Instant.now().plusSeconds(3600),
                JWSAlgorithm.HS512, TEST_SECRET);

        mockMvc.perform(get("/api/applications/recruiter/me")
                        .header("Authorization", candidateToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/applications/7/status")
                        .header("Authorization", candidateToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsUnsupportedApplicationStatus() throws Exception {
        mockMvc.perform(patch("/api/applications/7/status")
                        .header("Authorization", bearerToken("recruiter-41", "RECRUITER",
                                Instant.now().plusSeconds(3600), JWSAlgorithm.HS512, TEST_SECRET))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void allowsCorsPreflightWithoutJwtAuthentication(String requestedMethod) throws Exception {
        mockMvc.perform(options("/api/applications/me/summary")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", requestedMethod)
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type,Accept,Origin"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString(requestedMethod)))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Accept")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Origin")));

        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsCorsPreflightFromUnconfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/applications/me/summary")
                        .header("Origin", "http://localhost:4201")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(applicationService);
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        byte[] incorrectKey = new byte[TEST_SECRET.length];
        new SecureRandom().nextBytes(incorrectKey);

        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "CANDIDATE", Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS512, incorrectKey)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "CANDIDATE", Instant.now().minusSeconds(60),
                                JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAlgorithmThatDoesNotMatchConfiguredKeyLength() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "CANDIDATE", Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS256, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutSubject() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken(null, "CANDIDATE",
                                Instant.now().plusSeconds(3600), JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsSubjectThatIsNotACandidateId() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("not-a-candidate-id", "CANDIDATE",
                                Instant.now().plusSeconds(3600), JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCandidateJwtWithZeroSubject() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("0", "CANDIDATE",
                                Instant.now().plusSeconds(3600), JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCandidateJwtWithNegativeSubject() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("-1", "CANDIDATE",
                                Instant.now().plusSeconds(3600), JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutRole() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", null, Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithInvalidRole() throws Exception {
        mockMvc.perform(get("/api/applications/me/summary")
                        .header("Authorization", bearerToken("41", "ADMIN", Instant.now().plusSeconds(3600),
                                JWSAlgorithm.HS512, TEST_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    private String bearerToken(
            String subject,
            String role,
            Instant expiration,
            JWSAlgorithm algorithm,
            byte[] signingKey
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .expirationTime(Date.from(expiration));
        if (subject != null) {
            claims.subject(subject);
        }
        if (role != null) {
            claims.claim("role", role);
        }
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).build(),
                claims.build()
        );
        token.sign(new MACSigner(signingKey));
        return "Bearer " + token.serialize();
    }

}
