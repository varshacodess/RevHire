package com.revhire.profile.mapper;

import com.revhire.profile.dto.RecruiterProfileRequest;
import com.revhire.profile.dto.RecruiterProfileResponse;
import com.revhire.profile.entity.RecruiterProfile;
import org.springframework.stereotype.Component;

@Component
public class RecruiterProfileMapper {
    public RecruiterProfile toEntity(Long userId, RecruiterProfileRequest request) {
        return new RecruiterProfile(userId, request.recruiterName().trim(), request.organizationName(),
                request.jobTitle(), request.location(), request.organizationDescription(),
                request.websiteUrl(), request.phone());
    }

    public void update(RecruiterProfile profile, RecruiterProfileRequest request) {
        profile.update(request.recruiterName().trim(), request.organizationName(), request.jobTitle(), request.location(),
                request.organizationDescription(), request.websiteUrl(), request.phone());
    }

    public RecruiterProfileResponse toResponse(RecruiterProfile profile) {
        return new RecruiterProfileResponse(profile.getId(), profile.getUserId(), profile.getRecruiterName(),
                profile.getOrganizationName(), profile.getJobTitle(), profile.getLocation(), profile.getOrganizationDescription(),
                profile.getWebsiteUrl(), profile.getPhone());
    }
}
