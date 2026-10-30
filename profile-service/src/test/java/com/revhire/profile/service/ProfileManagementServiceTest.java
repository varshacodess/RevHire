package com.revhire.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revhire.profile.dto.CandidateProfileRequest;
import com.revhire.profile.dto.CandidateProfileResponse;
import com.revhire.profile.dto.EducationRequest;
import com.revhire.profile.dto.ExperienceRequest;
import com.revhire.profile.dto.RecruiterProfileRequest;
import com.revhire.profile.dto.ResumeRequest;
import com.revhire.profile.dto.SkillRequest;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Education;
import com.revhire.profile.entity.Experience;
import com.revhire.profile.entity.RecruiterProfile;
import com.revhire.profile.entity.Resume;
import com.revhire.profile.entity.Skill;
import com.revhire.profile.exception.DuplicateResourceException;
import com.revhire.profile.mapper.CandidateProfileMapper;
import com.revhire.profile.mapper.EducationMapper;
import com.revhire.profile.mapper.ExperienceMapper;
import com.revhire.profile.mapper.RecruiterProfileMapper;
import com.revhire.profile.mapper.ResumeMapper;
import com.revhire.profile.mapper.SkillMapper;
import com.revhire.profile.repository.CandidateProfileRepository;
import com.revhire.profile.repository.EducationRepository;
import com.revhire.profile.repository.ExperienceRepository;
import com.revhire.profile.repository.RecruiterProfileRepository;
import com.revhire.profile.repository.ResumeRepository;
import com.revhire.profile.repository.SkillRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileManagementServiceTest {
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private ExperienceRepository experienceRepository;
    @Mock private ResumeRepository resumeRepository;

    private CandidateProfileService candidateProfileService;
    private RecruiterProfileService recruiterProfileService;
    private SkillService skillService;
    private EducationService educationService;
    private ExperienceService experienceService;
    private ResumeService resumeService;
    private CandidateProfile candidateProfile;

    @BeforeEach
    void setUp() {
        candidateProfile = new CandidateProfile(17L, "Alex", "Candidate", "5551234567",
                "Seattle", "Engineer", "Summary", null, null);
        candidateProfileService = new CandidateProfileService(candidateProfileRepository, new CandidateProfileMapper());
        recruiterProfileService = new RecruiterProfileService(recruiterProfileRepository, new RecruiterProfileMapper());
        skillService = new SkillService(candidateProfileService, skillRepository, new SkillMapper());
        educationService = new EducationService(candidateProfileService, educationRepository, new EducationMapper());
        experienceService = new ExperienceService(candidateProfileService, experienceRepository, new ExperienceMapper());
        resumeService = new ResumeService(candidateProfileService, resumeRepository, new ResumeMapper());
    }

    @Test
    void candidateProfileCanBeCreatedRetrievedAndUpdated() {
        CandidateProfileRequest request = new CandidateProfileRequest("Alex", "Candidate", "5551234567",
                "Seattle", "Engineer", "Summary", null, null);
        when(candidateProfileRepository.existsByUserId(17L)).thenReturn(false);
        when(candidateProfileRepository.save(any(CandidateProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateProfileRepository.findByUserId(17L)).thenReturn(Optional.of(candidateProfile));

        assertEquals(17L, candidateProfileService.create(17L, request).userId());
        assertEquals("Engineer", candidateProfileService.get(17L).headline());
        CandidateProfileResponse updated = candidateProfileService.update(17L,
                new CandidateProfileRequest("Alex", "Candidate", "5551234567",
                        "Seattle", "Senior Engineer", "Updated", null, null));
        assertEquals("Senior Engineer", updated.headline());
    }

    @Test
    void duplicateCandidateProfileIsRejected() {
        when(candidateProfileRepository.existsByUserId(17L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> candidateProfileService.create(17L,
                new CandidateProfileRequest("Alex", null, null, null, null, null, null, null)));
    }

    @Test
    void skillCanBeCreatedAndUpdatedOnlyThroughOwnerScopedLookup() {
        when(candidateProfileRepository.findByUserId(17L)).thenReturn(Optional.of(candidateProfile));
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Skill skill = new Skill(candidateProfile, "Java", "Intermediate");
        when(skillRepository.findByIdAndCandidateProfileUserId(4L, 17L)).thenReturn(Optional.of(skill));

        assertEquals("Java", skillService.create(17L, new SkillRequest("Java", "Intermediate")).name());
        assertEquals("Advanced", skillService.update(17L, 4L, new SkillRequest("Java", "Advanced")).proficiency());
    }

    @Test
    void educationCanBeCreatedAndUpdated() {
        when(candidateProfileRepository.findByUserId(17L)).thenReturn(Optional.of(candidateProfile));
        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Education education = new Education(candidateProfile, "College", "BS", "CS",
                LocalDate.of(2020, 1, 1), null);
        when(educationRepository.findByIdAndCandidateProfileUserId(6L, 17L)).thenReturn(Optional.of(education));
        EducationRequest request = new EducationRequest("College", "BS", "CS", LocalDate.of(2020, 1, 1), null);

        assertEquals("College", educationService.create(17L, request).institution());
        assertEquals("MS", educationService.update(17L, 6L,
                new EducationRequest("College", "MS", "CS", LocalDate.of(2020, 1, 1), null)).degree());
    }

    @Test
    void experienceCanBeCreatedAndUpdated() {
        when(candidateProfileRepository.findByUserId(17L)).thenReturn(Optional.of(candidateProfile));
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Experience experience = new Experience(candidateProfile, "Company", "Engineer", "Seattle", null,
                LocalDate.of(2022, 1, 1), null);
        when(experienceRepository.findByIdAndCandidateProfileUserId(8L, 17L)).thenReturn(Optional.of(experience));
        ExperienceRequest request = new ExperienceRequest("Company", "Engineer", "Seattle", null,
                LocalDate.of(2022, 1, 1), null);

        assertEquals("Company", experienceService.create(17L, request).companyName());
        assertEquals("Senior Engineer", experienceService.update(17L, 8L,
                new ExperienceRequest("Company", "Senior Engineer", "Seattle", null,
                        LocalDate.of(2022, 1, 1), null)).jobTitle());
    }

    @Test
    void resumeCanBeCreatedRetrievedUpdatedAndDeleted() {
        when(candidateProfileRepository.findByUserId(17L)).thenReturn(Optional.of(candidateProfile));
        when(resumeRepository.findByCandidateProfileUserId(17L)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Resume(candidateProfile, "resume.pdf", "https://files/resume.pdf", "application/pdf")))
                .thenReturn(Optional.of(new Resume(candidateProfile, "resume.pdf", "https://files/resume.pdf", "application/pdf")));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("resume.pdf", resumeService.create(17L,
                new ResumeRequest("resume.pdf", "https://files/resume.pdf", "application/pdf")).fileName());
        assertEquals("resume.pdf", resumeService.get(17L).fileName());
        assertEquals("updated.pdf", resumeService.update(17L,
                new ResumeRequest("updated.pdf", "https://files/updated.pdf", "application/pdf")).fileName());
        resumeService.delete(17L);
        verify(resumeRepository).delete(any(Resume.class));
    }

    @Test
    void recruiterProfileCanBeCreatedRetrievedAndUpdated() {
        RecruiterProfileRequest request = new RecruiterProfileRequest(
                "Recruiter Name", "Example Org", "Talent Lead", "Austin", null, "https://example.com", null
        );
        when(recruiterProfileRepository.existsByUserId(24L)).thenReturn(false);
        when(recruiterProfileRepository.save(any(RecruiterProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recruiterProfileRepository.findByUserId(24L)).thenReturn(Optional.of(
                new RecruiterProfile(24L, "Recruiter Name", "Example Org", "Talent Lead", "Austin", null, null, null)
        ));

        assertEquals("Example Org", recruiterProfileService.create(24L, request).organizationName());
        assertEquals("Example Org", recruiterProfileService.get(24L).organizationName());
        assertEquals("Recruiting Manager", recruiterProfileService.update(24L,
                new RecruiterProfileRequest("Recruiter Name", "Example Org", "Recruiting Manager",
                        "Austin", null, null, null)).jobTitle());
    }
}
