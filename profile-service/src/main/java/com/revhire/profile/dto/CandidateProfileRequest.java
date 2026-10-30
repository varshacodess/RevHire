package com.revhire.profile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Pattern(regexp = "^[+0-9() .-]{7,20}$") String phone,
        @Size(max = 150) String location,
        @Size(max = 160) String headline,
        @Size(max = 2000) String summary,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 500) String portfolioUrl
) {
}
