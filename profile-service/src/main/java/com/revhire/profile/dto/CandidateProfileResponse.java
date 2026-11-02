package com.revhire.profile.dto;

public record CandidateProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String phone,
        String location,
        String headline,
        String summary,
        String linkedinUrl,
        String portfolioUrl
) {
}
