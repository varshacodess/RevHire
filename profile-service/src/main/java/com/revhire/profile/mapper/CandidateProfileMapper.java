package com.revhire.profile.mapper;

import com.revhire.profile.dto.CandidateProfileRequest;
import com.revhire.profile.dto.CandidateProfileResponse;
import com.revhire.profile.entity.CandidateProfile;
import org.springframework.stereotype.Component;

@Component
public class CandidateProfileMapper {
    public CandidateProfile toEntity(Long userId, CandidateProfileRequest request) {
        return new CandidateProfile(userId, request.firstName().trim(), request.lastName(), request.phone(),
                request.location(), request.headline(), request.summary(), request.linkedinUrl(),
                request.portfolioUrl());
    }

    public void update(CandidateProfile profile, CandidateProfileRequest request) {
        profile.update(request.firstName().trim(), request.lastName(), request.phone(), request.location(),
                request.headline(), request.summary(), request.linkedinUrl(), request.portfolioUrl());
    }

    public CandidateProfileResponse toResponse(CandidateProfile profile) {
        return new CandidateProfileResponse(profile.getId(), profile.getUserId(), profile.getFirstName(),
                profile.getLastName(), profile.getPhone(), profile.getLocation(), profile.getHeadline(), profile.getSummary(),
                profile.getLinkedinUrl(), profile.getPortfolioUrl());
    }
}
