package com.revhire.profile.service;

import com.revhire.profile.dto.SkillRequest;
import com.revhire.profile.dto.SkillResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Skill;
import com.revhire.profile.exception.ResourceNotFoundException;
import com.revhire.profile.mapper.SkillMapper;
import com.revhire.profile.repository.SkillRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {
    private final CandidateProfileService profileService;
    private final SkillRepository repository;
    private final SkillMapper mapper;

    public SkillService(CandidateProfileService profileService, SkillRepository repository, SkillMapper mapper) {
        this.profileService = profileService;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public SkillResponse create(Long userId, SkillRequest request) {
        CandidateProfile profile = profileService.findProfile(userId);
        return mapper.toResponse(repository.save(mapper.toEntity(profile, request)));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAll(Long userId) {
        profileService.findProfile(userId);
        return repository.findAllByCandidateProfileUserId(userId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public SkillResponse update(Long userId, Long id, SkillRequest request) {
        Skill skill = findOwned(userId, id);
        mapper.update(skill, request);
        return mapper.toResponse(repository.save(skill));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(findOwned(userId, id));
    }

    private Skill findOwned(Long userId, Long id) {
        return repository.findByIdAndCandidateProfileUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill"));
    }
}
