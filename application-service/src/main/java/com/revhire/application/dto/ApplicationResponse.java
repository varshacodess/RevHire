package com.revhire.application.dto;

import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long applicationId,
        Long candidateId,
        Long jobId,
        ApplicationStatus status,
        LocalDateTime appliedDate,
        LocalDateTime updatedDate
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getApplicationId(),
                application.getCandidateId(),
                application.getJobId(),
                application.getStatus(),
                application.getAppliedDate(),
                application.getUpdatedDate()
        );
    }
}
