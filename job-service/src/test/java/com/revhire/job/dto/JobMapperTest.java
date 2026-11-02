package com.revhire.job.dto;

import com.revhire.job.entity.Job;
import com.revhire.job.entity.JobStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobMapperTest {
    private final JobMapper mapper = new JobMapper();

    @Test
    void createsOpenJobAndDeduplicatesSkillsIgnoringCase() {
        Job job = mapper.toEntity(new CreateJobRequest(
                "Title", "Description", "Requirements", "Location",
                "FULL_TIME", 1, List.of("Java", " java ", "SQL")
        ));

        assertEquals(JobStatus.OPEN, job.getStatus());
        assertEquals(Set.of("Java", "SQL"), job.getSkills());
    }

    @Test
    void responseEligibilityReflectsConfiguredStatus() {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.CLOSED);
        job.setSkills(Set.of());

        JobResponse response = mapper.toResponse(job);

        assertTrue(response.exists());
        assertEquals(JobStatus.CLOSED, response.status());
        assertFalse(response.acceptingApplications());
    }
}
