package com.revhire.profile.mapper;

import com.revhire.profile.dto.ExperienceRequest;
import com.revhire.profile.dto.ExperienceResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Experience;
import org.springframework.stereotype.Component;

@Component
public class ExperienceMapper {
    public Experience toEntity(CandidateProfile profile, ExperienceRequest request) {
        return new Experience(profile, request.companyName().trim(), request.jobTitle().trim(), request.location(),
                request.description(), request.startDate(), request.endDate());
    }

    public void update(Experience experience, ExperienceRequest request) {
        experience.update(request.companyName().trim(), request.jobTitle().trim(), request.location(),
                request.description(), request.startDate(), request.endDate());
    }

    public ExperienceResponse toResponse(Experience experience) {
        return new ExperienceResponse(experience.getId(), experience.getCompanyName(), experience.getJobTitle(),
                experience.getLocation(), experience.getDescription(), experience.getStartDate(),
                experience.getEndDate());
    }
}
