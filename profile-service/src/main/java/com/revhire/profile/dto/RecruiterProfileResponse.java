package com.revhire.profile.dto;

public record RecruiterProfileResponse(
        Long id,
        Long userId,
        String recruiterName,
        String organizationName,
        String jobTitle,
        String location,
        String organizationDescription,
        String websiteUrl,
        String phone
) {
}
