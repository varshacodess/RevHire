package com.revhire.application.dto;

import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;

import java.time.LocalDateTime;

public record RecruiterApplicationResponse(
        Long applicationId,
        Long jobId,
        String jobTitle,
        Long candidateId,
        ApplicationStatus applicationStatus,
        LocalDateTime appliedDate,
        LocalDateTime updatedDate
) {
    public static RecruiterApplicationResponse from(Application application, String jobTitle) {
        return new RecruiterApplicationResponse(
                application.getApplicationId(),
                application.getJobId(),
                jobTitle,
                application.getCandidateId(),
                application.getStatus(),
                application.getAppliedDate(),
                application.getUpdatedDate()
        );
    }
}
