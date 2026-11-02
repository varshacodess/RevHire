package com.revhire.profile.service;

import com.revhire.profile.dto.EducationRequest;
import com.revhire.profile.dto.EducationResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Education;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.EducationMapper;
import com.revhire.profile.repository.EducationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationService {
    private final CandidateProfileService profileService;
    private final EducationRepository repository;
    private final EducationMapper mapper;

    public EducationService(CandidateProfileService profileService, EducationRepository repository, EducationMapper mapper) {
        this.profileService = profileService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public EducationResponse create(Long userId, EducationRequest request) {
        CandidateProfile profile = profileService.findProfile(userId);
        return mapper.toResponse(repository.save(mapper.toEntity(profile, request)));
    }

    @Transactional(readOnly = true)
    public List<EducationResponse> getAll(Long userId) {
        profileService.findProfile(userId);
        return repository.findAllByCandidateProfileUserId(userId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public EducationResponse update(Long userId, Long id, EducationRequest request) {
        Education education = findOwned(userId, id);
        mapper.update(education, request);
        return mapper.toResponse(repository.save(education));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(findOwned(userId, id));
    }

    private Education findOwned(Long userId, Long id) {
        return repository.findByIdAndCandidateProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Education"));
    }
}
