package com.revhire.job.dto;

import com.revhire.job.entity.JobStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record JobResponse(
        Long jobId,
        boolean exists,
        JobStatus status,
        boolean acceptingApplications,
        String title,
        String description,
        String requirements,
        String location,
        String employmentType,
        Integer experienceYears,
        Set<String> skills,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
