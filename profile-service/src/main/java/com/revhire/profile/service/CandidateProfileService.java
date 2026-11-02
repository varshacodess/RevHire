package com.revhire.profile.service;

import com.revhire.profile.dto.CandidateProfileRequest;
import com.revhire.profile.dto.CandidateProfileResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.exception.DuplicateResourceException;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.CandidateProfileMapper;
import com.revhire.profile.repository.CandidateProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateProfileService {
    private final CandidateProfileRepository profileRepository;
    private final CandidateProfileMapper mapper;

    public CandidateProfileService(CandidateProfileRepository profileRepository, CandidateProfileMapper mapper) {
        this.profileRepository = profileRepository;
        this.mapper = mapper;
    }

    @Transactional
    public CandidateProfileResponse create(Long userId, CandidateProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Candidate profile");
        }
        return mapper.toResponse(profileRepository.save(mapper.toEntity(userId, request)));
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse get(Long userId) {
        return mapper.toResponse(findProfile(userId));
    }

    @Transactional
    public CandidateProfileResponse update(Long userId, CandidateProfileRequest request) {
        CandidateProfile profile = findProfile(userId);
        mapper.update(profile, request);
        return mapper.toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public CandidateProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile"));
    }
}
