package com.revhire.authentication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revhire.authentication.dto.AuthResponse;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.RecruiterRegistrationRequest;
import com.revhire.authentication.dto.UserResponse;
import com.revhire.authentication.entity.Role;
import com.revhire.authentication.exception.InvalidCredentialsException;
import com.revhire.authentication.security.JwtService;
import com.revhire.authentication.security.SecurityConfig;
import com.revhire.authentication.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void candidateRegistrationIsPublicAndReturnsSafeResponse() throws Exception {
        when(authenticationService.registerCandidate(any(CandidateRegistrationRequest.class)))
                .thenReturn(new UserResponse(1L, "Candidate", "candidate@example.com", "candidate", Role.CANDIDATE));

        mockMvc.perform(post("/api/auth/candidate/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Candidate","email":"candidate@example.com",
                                 "username":"candidate","password":"securePass123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void recruiterRegistrationIsPublicAndReturnsSafeResponse() throws Exception {
        when(authenticationService.registerRecruiter(any(RecruiterRegistrationRequest.class)))
                .thenReturn(new UserResponse(2L, "Recruiter", "recruiter@example.com", "recruiter", Role.RECRUITER));

        mockMvc.perform(post("/api/auth/recruiter/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Recruiter","email":"recruiter@example.com",
                                 "username":"recruiter","password":"securePass123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginReturnsBearerTokenAndSafeUserDetails() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new AuthResponse("signed.jwt.token", "Bearer",
                        new UserResponse(1L, "Candidate", "candidate@example.com", "candidate", Role.CANDIDATE)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"candidate","password":"securePass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void invalidLoginIsRejected() throws Exception {
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"candidate","password":"wrongPassword123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email/username or password"));
    }

    @Test
    void registrationInputIsValidated() throws Exception {
        mockMvc.perform(post("/api/auth/candidate/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","username":"x","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recruiterRegistrationInputIsValidated() throws Exception {
        mockMvc.perform(post("/api/auth/recruiter/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"","email":"not-an-email","username":"x","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
