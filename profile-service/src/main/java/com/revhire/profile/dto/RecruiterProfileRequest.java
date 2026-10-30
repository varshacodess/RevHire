package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecruiterProfileRequest(
        @NotBlank @Size(max = 150) String recruiterName,
        @NotBlank @Size(max = 150) String organizationName,
        @Size(max = 120) String jobTitle,
        @Size(max = 120) String location,
        @Size(max = 2000) String organizationDescription,
        @Pattern(regexp = "^$|https?://.+") @Size(max = 500) String websiteUrl,
        @Pattern(regexp = "^[+0-9() .-]{7,20}$") String phone
) {
}
