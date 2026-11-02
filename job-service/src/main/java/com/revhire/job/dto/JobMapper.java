package com.revhire.job.dto;

import com.revhire.job.entity.Job;
import com.revhire.job.entity.JobStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class JobMapper {
    public Job toEntity(CreateJobRequest request) {
        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setDescription(request.description().trim());
        job.setRequirements(request.requirements().trim());
        job.setLocation(request.location().trim());
        job.setEmploymentType(request.employmentType().trim());
        job.setExperienceYears(request.experienceYears());
        job.setStatus(JobStatus.OPEN);
        job.setSkills(normalizeSkills(request.skills()));
        return job;
    }

    public void updateEntity(Job job, UpdateJobRequest request) {
        if (request.title() != null) {
            job.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            job.setDescription(request.description().trim());
        }
        if (request.requirements() != null) {
            job.setRequirements(request.requirements().trim());
        }
        if (request.location() != null) {
            job.setLocation(request.location().trim());
        }
        if (request.employmentType() != null) {
            job.setEmploymentType(request.employmentType().trim());
        }
        if (request.experienceYears() != null) {
            job.setExperienceYears(request.experienceYears());
        }
        if (request.skills() != null) {
            job.setSkills(normalizeSkills(request.skills()));
        }
    }

    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                true,
                job.getStatus(),
                job.getStatus() == JobStatus.OPEN,
                job.getTitle(),
                job.getDescription(),
                job.getRequirements(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getExperienceYears(),
                Set.copyOf(job.getSkills()),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private Set<String> normalizeSkills(Iterable<String> skills) {
        Set<String> normalized = new LinkedHashSet<>();
        Set<String> seen = new LinkedHashSet<>();
        if (skills == null) {
            return normalized;
        }
        for (String skill : skills) {
            if (skill != null) {
                String value = skill.trim();
                if (!value.isEmpty() && seen.add(value.toLowerCase(Locale.ROOT))) {
                    normalized.add(value);
                }
            }
        }
        return normalized;
    }
}
