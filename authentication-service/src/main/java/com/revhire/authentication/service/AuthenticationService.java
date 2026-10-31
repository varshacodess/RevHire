package com.revhire.authentication.service;

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
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse registerCandidate(CandidateRegistrationRequest request) {
        return register(request.fullName(), request.email(), request.username(), request.password(), Role.CANDIDATE);
    }

    @Transactional
    public UserResponse registerRecruiter(RecruiterRegistrationRequest request) {
        return register(request.fullName(), request.email(), request.username(), request.password(), Role.RECRUITER);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();

        User user = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(InvalidCredentialsException::new);

        System.out.println("USER FOUND: " + user.getUsername());

        System.out.println("PASSWORD MATCH: " +
                passwordEncoder.matches(request.password(), user.getPasswordHash()));

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                userResponse);
    }

    private UserResponse register(String fullName, String email, String username, String password, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail) || userRepository.existsByUsername(normalizedUsername)) {
            throw new DuplicateUserException();
        }

        User user = userMapper.toEntity(
                fullName.trim(),
                normalizedEmail,
                normalizedUsername,
                passwordEncoder.encode(password),
                role
        );
        return userMapper.toResponse(userRepository.save(user));
    }
}
