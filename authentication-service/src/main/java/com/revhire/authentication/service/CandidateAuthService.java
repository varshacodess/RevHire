package com.revhire.authentication.service;

import com.revhire.authentication.dto.CandidateLoginRequest;
import com.revhire.authentication.dto.CandidateRegistrationRequest;
import com.revhire.authentication.dto.CandidateResponse;
import com.revhire.authentication.entity.Candidate;
import com.revhire.authentication.exception.DuplicateCandidateException;
import com.revhire.authentication.exception.InvalidCredentialsException;
import com.revhire.authentication.repository.CandidateRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateAuthService {
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    public CandidateAuthService(CandidateRepository candidateRepository, PasswordEncoder passwordEncoder) {
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CandidateResponse register(CandidateRegistrationRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        if (candidateRepository.existsByEmail(email) || candidateRepository.existsByUsername(username)) {
            throw new DuplicateCandidateException();
        }

        Candidate candidate = new Candidate(
                request.fullName().trim(),
                email,
                username,
                passwordEncoder.encode(request.password())
        );
        return toResponse(candidateRepository.save(candidate));
    }

    @Transactional(readOnly = true)
    public CandidateResponse login(CandidateLoginRequest request) {
        String identifier = request.identifier().trim();
        Candidate candidate = candidateRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return toResponse(candidate);
    }

    private CandidateResponse toResponse(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getFullName(),
                candidate.getEmail(),
                candidate.getUsername()
        );
    }
}
