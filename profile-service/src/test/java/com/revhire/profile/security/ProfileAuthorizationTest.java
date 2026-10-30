package com.revhire.profile.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revhire.profile.controller.CandidateProfileController;
import com.revhire.profile.controller.RecruiterProfileController;
import com.revhire.profile.dto.CandidateProfileRequest;
import com.revhire.profile.dto.CandidateProfileResponse;
import com.revhire.profile.dto.RecruiterProfileResponse;
import com.revhire.profile.service.CandidateProfileService;
import com.revhire.profile.service.EducationService;
import com.revhire.profile.service.ExperienceService;
import com.revhire.profile.service.RecruiterProfileService;
import com.revhire.profile.service.ResumeService;
import com.revhire.profile.service.SkillService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {CandidateProfileController.class, RecruiterProfileController.class})
@Import(SecurityConfig.class)
class ProfileAuthorizationTest {
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private CandidateProfileService candidateProfileService;
    @MockitoBean private SkillService skillService;
    @MockitoBean private EducationService educationService;
    @MockitoBean private ExperienceService experienceService;
    @MockitoBean private ResumeService resumeService;
    @MockitoBean private RecruiterProfileService recruiterProfileService;

    @Test
    void candidateCanCreateOwnProfileWithoutSpecifyingAccountId() throws Exception {
        when(candidateProfileService.create(eq(42L), any(CandidateProfileRequest.class)))
                .thenReturn(new CandidateProfileResponse(1L, 42L, "Alex", null, null,
                        null, null, null, null, null));

        mockMvc.perform(post("/api/candidates/profile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("42").roles("CANDIDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alex\",\"headline\":\"Engineer\",\"userId\":999}"))
                .andExpect(status().isCreated());

        verify(candidateProfileService).create(eq(42L), any(CandidateProfileRequest.class));
    }

    @Test
    void validCandidateJwtSuppliesProfileIdentity() throws Exception {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "CANDIDATE")
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET)))
                .compact();
        JwtService parser = new JwtService(TEST_SECRET);
        when(jwtService.parseToken(token)).thenReturn(parser.parseToken(token));
        when(candidateProfileService.get(42L))
                .thenReturn(new CandidateProfileResponse(1L, 42L, "Alex", null, null,
                        null, null, null, null, null));

        mockMvc.perform(get("/api/candidates/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(candidateProfileService).get(42L);
    }

    @Test
    void unauthenticatedCandidateProfileRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/candidates/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "42", roles = "RECRUITER")
    void recruiterCannotAccessCandidateProfile() throws Exception {
        mockMvc.perform(get("/api/candidates/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "24", roles = "RECRUITER")
    void recruiterCanAccessOwnProfile() throws Exception {
        when(recruiterProfileService.get(24L))
                .thenReturn(new RecruiterProfileResponse(3L, 24L, "Recruiter", "Org",
                        null, null, null, null, null));

        mockMvc.perform(get("/api/recruiters/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "42", roles = "CANDIDATE")
    void nonAdminCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isForbidden());
    }
}
