package com.revhire.profile.service;

import com.revhire.profile.dto.ResumeRequest;
import com.revhire.profile.dto.ResumeResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.exception.DuplicateResourceException;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.ResumeMapper;
import com.revhire.profile.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeService {
    private final CandidateProfileService profileService;
    private final ResumeRepository repository;
    private final ResumeMapper mapper;

    public ResumeService(CandidateProfileService profileService, ResumeRepository repository, ResumeMapper mapper) {
        this.profileService = profileService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ResumeResponse create(Long userId, ResumeRequest request) {
        CandidateProfile profile = profileService.findProfile(userId);
        if (repository.findByCandidateProfileUserId(userId).isPresent()) {
            throw new DuplicateResourceException("Resume");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(profile, request)));
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(Long userId) {
        profileService.findProfile(userId);
        return mapper.toResponse(findOwned(userId));
    }

    @Transactional
    public ResumeResponse update(Long userId, ResumeRequest request) {
        Resume resume = findOwned(userId);
        mapper.update(resume, request);
        return mapper.toResponse(repository.save(resume));
    }

    @Transactional
    public void delete(Long userId) {
        repository.delete(findOwned(userId));
    }

    private Resume findOwned(Long userId) {
        return repository.findByCandidateProfileUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume"));
    }
}
