package com.revhire.profile.controller;

import com.revhire.profile.dto.CandidateProfileRequest;
import com.revhire.profile.dto.CandidateProfileResponse;
import com.revhire.profile.dto.EducationRequest;
import com.revhire.profile.dto.EducationResponse;
import com.revhire.profile.dto.ExperienceRequest;
import com.revhire.profile.dto.ExperienceResponse;
import com.revhire.profile.dto.ResumeRequest;
import com.revhire.profile.dto.ResumeResponse;
import com.revhire.profile.dto.SkillRequest;
import com.revhire.profile.dto.SkillResponse;
import com.revhire.profile.service.CandidateProfileService;
import com.revhire.profile.service.EducationService;
import com.revhire.profile.service.ExperienceService;
import com.revhire.profile.service.ResumeService;
import com.revhire.profile.service.SkillService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates/profile")
public class CandidateProfileController {
    private final CandidateProfileService profileService;
    private final SkillService skillService;
    private final EducationService educationService;
    private final ExperienceService experienceService;
    private final ResumeService resumeService;

    public CandidateProfileController(
            CandidateProfileService profileService,
            SkillService skillService,
            EducationService educationService,
            ExperienceService experienceService,
            ResumeService resumeService
    ) {
        this.profileService = profileService;
        this.skillService = skillService;
        this.educationService = educationService;
        this.experienceService = experienceService;
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<CandidateProfileResponse> create(
            Authentication authentication,
            @Valid @RequestBody CandidateProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.create(userId(authentication), request));
    }

    @GetMapping
    public CandidateProfileResponse get(Authentication authentication) {
        return profileService.get(userId(authentication));
    }

    @PutMapping
    public CandidateProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody CandidateProfileRequest request
    ) {
        return profileService.update(userId(authentication), request);
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillResponse> createSkill(
            Authentication authentication,
            @Valid @RequestBody SkillRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.create(userId(authentication), request));
    }

    @GetMapping("/skills")
    public List<SkillResponse> getSkills(Authentication authentication) {
        return skillService.getAll(userId(authentication));
    }

    @PutMapping("/skills/{id}")
    public SkillResponse updateSkill(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SkillRequest request
    ) {
        return skillService.update(userId(authentication), id, request);
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(Authentication authentication, @PathVariable Long id) {
        skillService.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/education")
    public ResponseEntity<EducationResponse> createEducation(
            Authentication authentication,
            @Valid @RequestBody EducationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(educationService.create(userId(authentication), request));
    }

    @GetMapping("/education")
    public List<EducationResponse> getEducation(Authentication authentication) {
        return educationService.getAll(userId(authentication));
    }

    @PutMapping("/education/{id}")
    public EducationResponse updateEducation(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody EducationRequest request
    ) {
        return educationService.update(userId(authentication), id, request);
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<Void> deleteEducation(Authentication authentication, @PathVariable Long id) {
        educationService.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/experience")
    public ResponseEntity<ExperienceResponse> createExperience(
            Authentication authentication,
            @Valid @RequestBody ExperienceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(experienceService.create(userId(authentication), request));
    }

    @GetMapping("/experience")
    public List<ExperienceResponse> getExperience(Authentication authentication) {
        return experienceService.getAll(userId(authentication));
    }

    @PutMapping("/experience/{id}")
    public ExperienceResponse updateExperience(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ExperienceRequest request
    ) {
        return experienceService.update(userId(authentication), id, request);
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<Void> deleteExperience(Authentication authentication, @PathVariable Long id) {
        experienceService.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resume")
    public ResponseEntity<ResumeResponse> createResume(
            Authentication authentication,
            @Valid @RequestBody ResumeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.create(userId(authentication), request));
    }

    @GetMapping("/resume")
    public ResumeResponse getResume(Authentication authentication) {
        return resumeService.get(userId(authentication));
    }

    @PutMapping("/resume")
    public ResumeResponse updateResume(
            Authentication authentication,
            @Valid @RequestBody ResumeRequest request
    ) {
        return resumeService.update(userId(authentication), request);
    }

    @DeleteMapping("/resume")
    public ResponseEntity<Void> deleteResume(Authentication authentication) {
        resumeService.delete(userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
