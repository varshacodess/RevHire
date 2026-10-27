package com.revhire.authentication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revhire.authentication.dto.CandidateLoginRequest;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.CandidateResponse;
import com.revhire.authentication.entity.Candidate;
import com.revhire.authentication.exception.DuplicateCandidateException;
import com.revhire.authentication.exception.InvalidCredentialsException;
import com.revhire.authentication.repository.CandidateRepository;
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
class CandidateAuthServiceTest {
    @Mock
    private CandidateRepository candidateRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private CandidateAuthService candidateAuthService;

    @BeforeEach
    void setUp() {
        candidateAuthService = new CandidateAuthService(candidateRepository, passwordEncoder);
    }

    @Test
    void registerStoresHashedPasswordAndReturnsPasswordFreeResponse() {
        CandidateRegistrationRequest request = new CandidateRegistrationRequest(
                "Candidate Name", "Candidate@Example.com", "CandidateUser", "strongPassword1"
        );
        when(candidateRepository.existsByEmail("candidate@example.com")).thenReturn(false);
        when(candidateRepository.existsByUsername("candidateuser")).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CandidateResponse response = candidateAuthService.register(request);

        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(candidateCaptor.capture());
        Candidate saved = candidateCaptor.getValue();
        assertEquals("candidate@example.com", saved.getEmail());
        assertEquals("candidateuser", saved.getUsername());
        assertTrue(passwordEncoder.matches(request.password(), saved.getPasswordHash()));
        assertNotEquals(request.password(), saved.getPasswordHash());
        assertEquals("Candidate Name", response.fullName());
        assertEquals("candidate@example.com", response.email());
    }

    @Test
    void registerRejectsDuplicateEmailOrUsername() {
        when(candidateRepository.existsByEmail("candidate@example.com")).thenReturn(true);

        assertThrows(DuplicateCandidateException.class, () -> candidateAuthService.register(
                new CandidateRegistrationRequest(
                        "Candidate Name", "candidate@example.com", "candidateuser", "strongPassword1"
                )
        ));

        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    void loginAcceptsMatchingPasswordAndReturnsPasswordFreeResponse() {
        Candidate candidate = new Candidate(
                "Candidate Name", "candidate@example.com", "candidateuser", passwordEncoder.encode("strongPassword1")
        );
        when(candidateRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("candidateuser", "candidateuser"))
                .thenReturn(Optional.of(candidate));

        CandidateResponse response = candidateAuthService.login(
                new CandidateLoginRequest("candidateuser", "strongPassword1")
        );

        assertEquals("candidateuser", response.username());
    }

    @Test
    void loginRejectsInvalidPassword() {
        Candidate candidate = new Candidate(
                "Candidate Name", "candidate@example.com", "candidateuser", passwordEncoder.encode("strongPassword1")
        );
        when(candidateRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("candidateuser", "candidateuser"))
                .thenReturn(Optional.of(candidate));

        assertThrows(InvalidCredentialsException.class, () -> candidateAuthService.login(
                new CandidateLoginRequest("candidateuser", "wrongPassword1")
        ));
    }
}
