package com.revhire.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revhire.authentication.dto.AuthResponse;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.LoginRequest;
import com.revhire.authentication.dto.RecruiterRegistrationRequest;
import com.revhire.authentication.dto.UserResponse;
import com.revhire.authentication.entity.Role;
import com.revhire.authentication.entity.User;
import com.revhire.authentication.exception.DuplicateUserException;
import com.revhire.authentication.exception.InvalidCredentialsException;
import com.revhire.authentication.mapper.UserMapper;
import com.revhire.authentication.repository.UserRepository;
import com.revhire.authentication.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, new UserMapper(), jwtService);
    }

    @Test
    void candidateRegistrationHashesPasswordAndAssignsCandidateRole() {
        when(userRepository.existsByEmail("candidate@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("candidate")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authenticationService.registerCandidate(new CandidateRegistrationRequest(
                "Candidate Name", "Candidate@Example.com", "Candidate", "securePass123"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(Role.CANDIDATE, saved.getRole());
        assertTrue(passwordEncoder.matches("securePass123", saved.getPasswordHash()));
        assertNotEquals("securePass123", saved.getPasswordHash());
        assertEquals(Role.CANDIDATE, response.role());
    }

    @Test
    void duplicateCandidateRegistrationIsRejected() {
        when(userRepository.existsByEmail("candidate@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authenticationService.registerCandidate(
                new CandidateRegistrationRequest("Candidate Name", "candidate@example.com", "candidate", "securePass123")
        ));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void recruiterRegistrationHashesPasswordAndAssignsRecruiterRole() {
        when(userRepository.existsByEmail("recruiter@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("recruiter")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authenticationService.registerRecruiter(new RecruiterRegistrationRequest(
                "Recruiter Name", "Recruiter@Example.com", "Recruiter", "securePass123"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(Role.RECRUITER, saved.getRole());
        assertTrue(passwordEncoder.matches("securePass123", saved.getPasswordHash()));
        assertNotEquals("securePass123", saved.getPasswordHash());
        assertEquals(Role.RECRUITER, response.role());
    }

    @Test
    void duplicateRecruiterEmailIsRejected() {
        when(userRepository.existsByEmail("recruiter@example.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authenticationService.registerRecruiter(
                new RecruiterRegistrationRequest("Recruiter Name", "recruiter@example.com", "recruiter", "securePass123")
        ));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void duplicateRecruiterUsernameIsRejected() {
        when(userRepository.existsByEmail("recruiter@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("recruiter")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authenticationService.registerRecruiter(
                new RecruiterRegistrationRequest("Recruiter Name", "recruiter@example.com", "recruiter", "securePass123")
        ));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void recruiterCanLogInAfterRegistration() {
        when(userRepository.existsByEmail("recruiter@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("recruiter")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authenticationService.registerRecruiter(new RecruiterRegistrationRequest(
                "Recruiter Name", "recruiter@example.com", "recruiter", "securePass123"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User recruiter = captor.getValue();
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("recruiter", "recruiter"))
                .thenReturn(Optional.of(recruiter));
        when(jwtService.generateToken(recruiter)).thenReturn("recruiter.jwt.token");

        AuthResponse response = authenticationService.login(new LoginRequest("recruiter", "securePass123"));

        assertEquals("recruiter.jwt.token", response.token());
        assertEquals(Role.RECRUITER, response.user().role());
    }

    @Test
    void loginReturnsJwtAndSafeUserDetails() {
        User user = new User("Candidate Name", "candidate@example.com", "candidate",
                passwordEncoder.encode("securePass123"), Role.CANDIDATE);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("candidate", "candidate"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");

        AuthResponse response = authenticationService.login(new LoginRequest("candidate", "securePass123"));

        assertEquals("signed.jwt.token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(Role.CANDIDATE, response.user().role());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        User user = new User("Candidate Name", "candidate@example.com", "candidate",
                passwordEncoder.encode("securePass123"), Role.CANDIDATE);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("candidate", "candidate"))
                .thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authenticationService.login(
                new LoginRequest("candidate", "wrongPassword123")
        ));
    }
}
