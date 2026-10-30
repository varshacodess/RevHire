package com.revhire.profile.service;

import com.revhire.profile.dto.RecruiterProfileRequest;
import com.revhire.profile.dto.RecruiterProfileResponse;
import com.revhire.profile.entity.RecruiterProfile;
import com.revhire.profile.exception.DuplicateResourceException;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.RecruiterProfileMapper;
import com.revhire.profile.repository.RecruiterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterProfileService {
    private final RecruiterProfileRepository profileRepository;
    private final RecruiterProfileMapper mapper;

    public RecruiterProfileService(RecruiterProfileRepository profileRepository, RecruiterProfileMapper mapper) {
        this.profileRepository = profileRepository;
        this.mapper = mapper;
    }

    @Transactional
    public RecruiterProfileResponse create(Long userId, RecruiterProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Recruiter profile");
        }
        return mapper.toResponse(profileRepository.save(mapper.toEntity(userId, request)));
    }

    @Transactional(readOnly = true)
    public RecruiterProfileResponse get(Long userId) {
        return mapper.toResponse(findProfile(userId));
    }

    @Transactional
    public RecruiterProfileResponse update(Long userId, RecruiterProfileRequest request) {
        RecruiterProfile profile = findProfile(userId);
        mapper.update(profile, request);
        return mapper.toResponse(profileRepository.save(profile));
    }

    private RecruiterProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile"));
    }
}
