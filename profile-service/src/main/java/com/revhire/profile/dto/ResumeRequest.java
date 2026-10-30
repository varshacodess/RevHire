package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResumeRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Pattern(regexp = "^https?://\\S+$") @Size(max = 1000) String fileUrl,
        @Size(max = 100) String contentType
) {
}
