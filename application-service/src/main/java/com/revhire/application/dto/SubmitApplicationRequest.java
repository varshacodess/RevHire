package com.revhire.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitApplicationRequest(
        @NotNull(message = "jobId is required")
        @Positive(message = "jobId must be greater than zero")
        Long jobId
) {
}
