package com.revhire.profile.service;

import com.revhire.profile.dto.ExperienceRequest;
import com.revhire.profile.dto.ExperienceResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Experience;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.ExperienceMapper;
import com.revhire.profile.repository.ExperienceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperienceService {
    private final CandidateProfileService profileService;
    private final ExperienceRepository repository;
    private final ExperienceMapper mapper;

    public ExperienceService(
            CandidateProfileService profileService,
            ExperienceRepository repository,
            ExperienceMapper mapper
    ) {
        this.profileService = profileService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ExperienceResponse create(Long userId, ExperienceRequest request) {
        CandidateProfile profile = profileService.findProfile(userId);
        return mapper.toResponse(repository.save(mapper.toEntity(profile, request)));
    }

    @Transactional(readOnly = true)
    public List<ExperienceResponse> getAll(Long userId) {
        profileService.findProfile(userId);
        return repository.findAllByCandidateProfileUserId(userId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public ExperienceResponse update(Long userId, Long id, ExperienceRequest request) {
        Experience experience = findOwned(userId, id);
        mapper.update(experience, request);
        return mapper.toResponse(repository.save(experience));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(findOwned(userId, id));
    }

    private Experience findOwned(Long userId, Long id) {
        return repository.findByIdAndCandidateProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience"));
    }
}
