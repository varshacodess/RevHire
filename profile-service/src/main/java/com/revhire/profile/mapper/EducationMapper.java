package com.revhire.profile.mapper;

import com.revhire.profile.dto.EducationRequest;
import com.revhire.profile.dto.EducationResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Education;
import org.springframework.stereotype.Component;

@Component
public class EducationMapper {
    public Education toEntity(CandidateProfile profile, EducationRequest request) {
        return new Education(profile, request.institution().trim(), request.degree().trim(),
                request.fieldOfStudy(), request.startDate(), request.endDate());
    }

    public void update(Education education, EducationRequest request) {
        education.update(request.institution().trim(), request.degree().trim(), request.fieldOfStudy(),
                request.startDate(), request.endDate());
    }

    public EducationResponse toResponse(Education education) {
        return new EducationResponse(education.getId(), education.getInstitution(), education.getDegree(),
                education.getFieldOfStudy(), education.getStartDate(), education.getEndDate());
    }
}
