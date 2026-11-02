package com.revhire.application.dto;

import com.revhire.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "status is required")
        ApplicationStatus status
) {
}
