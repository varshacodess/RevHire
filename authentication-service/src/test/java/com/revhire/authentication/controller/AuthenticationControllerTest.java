package com.revhire.authentication.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revhire.authentication.dto.AuthResponse;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.RecruiterRegistrationRequest;
import com.revhire.authentication.dto.UserResponse;
import com.revhire.authentication.entity.Role;
import com.revhire.authentication.entity.User;
import com.revhire.authentication.exception.InvalidCredentialsException;
import com.revhire.authentication.security.JwtService;
import com.revhire.authentication.security.SecurityConfig;
import com.revhire.authentication.service.AuthenticationService;
import io.jsonwebtoken.JwtException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, AuthenticationControllerTest.ProtectedEndpointConfiguration.class})
class AuthenticationControllerTest {
    private static final String JWT_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    private final JwtService tokenIssuer = new JwtService(JWT_SECRET, 60_000);

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
    void loginWithNonBearerAuthorizationHeaderRemainsPublic() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new AuthResponse("signed.jwt.token", "Bearer",
                        new UserResponse(1L, "Candidate", "candidate@example.com", "candidate", Role.CANDIDATE)));

        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Basic dXNlcjpwYXNz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"candidate","password":"securePass123"}
                                """))
                .andExpect(status().isOk());
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

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void protectedEndpointRejectsInvalidBearerToken() throws Exception {
        when(jwtService.parseToken(anyString())).thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired bearer token"));
    }

    @Test
    void protectedEndpointRejectsExpiredBearerToken() throws Exception {
        JwtService expiredTokenIssuer = new JwtService(JWT_SECRET, -1);
        String expiredToken = expiredTokenIssuer.generateToken(userWithId(42L));
        when(jwtService.parseToken(expiredToken)).thenAnswer(invocation ->
                tokenIssuer.parseToken(invocation.getArgument(0)));

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired bearer token"));
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        String validToken = tokenIssuer.generateToken(userWithId(42L));
        when(jwtService.parseToken(validToken)).thenAnswer(invocation ->
                tokenIssuer.parseToken(invocation.getArgument(0)));

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(content().string("protected"));
    }

    private User userWithId(Long id) {
        User user = new User("Candidate", "candidate@example.com", "candidate", "hash", Role.CANDIDATE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @TestConfiguration
    static class ProtectedEndpointConfiguration {
        @Bean
        ProtectedTestController protectedTestController() {
            return new ProtectedTestController();
        }
    }

    @RestController
    static class ProtectedTestController {
        @GetMapping("/test/protected")
        String protectedEndpoint() {
            return "protected";
        }
    }
}
