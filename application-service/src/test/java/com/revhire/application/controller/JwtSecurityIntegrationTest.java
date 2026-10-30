package com.revhire.application.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.revhire.application.config.JwtSecurityConfiguration;
import com.revhire.application.service.ApplicationService;
import com.revhire.application.service.SecurityContextCandidateIdentityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
