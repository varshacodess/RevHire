package com.revhire.job.service;

import com.revhire.job.dto.CreateJobRequest;
import com.revhire.job.dto.JobMapper;
import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.dto.UpdateJobRequest;
import com.revhire.job.entity.Job;
import com.revhire.job.entity.JobStatus;
import com.revhire.job.exception.InvalidJobSearchException;
import com.revhire.job.exception.JobNotFoundException;
import com.revhire.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock
    private JobRepository repository;

    private JobService service;

    private JobMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobMapper();
        service = new JobService(repository, mapper);
    }

    @Test
    void createDefaultsToOpenAndMapsSkills() {
        when(repository.save(any(Job.class))).thenAnswer(invocation -> {
            Job saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        var response = service.create(new CreateJobRequest(
                " Engineer ", " Description ", " Requirements ", " Remote ",
                " Full time ", 2, List.of("Java", " java ", "Spring")
        ));

        assertEquals(11L, response.jobId());
        assertEquals(JobStatus.OPEN, response.status());
        assertTrue(response.acceptingApplications());
        assertEquals("Engineer", response.title());
        assertEquals(2, response.skills().size());
    }

    @Test
    void updatePreservesOmittedValues() {
        Job job = job(12L, JobStatus.OPEN);
        job.setTitle("Original");
        when(repository.findById(12L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        var response = service.update(12L, new UpdateJobRequest(
                "Updated", null, null, null, null, null, null
        ));

        assertEquals("Updated", response.title());
        assertEquals("description", response.description());
        assertEquals("location", response.location());
        verify(repository).save(job);
    }

    @Test
    void closeMakesJobIneligibleForApplications() {
        Job job = job(13L, JobStatus.OPEN);
        when(repository.findById(13L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        var response = service.close(13L);

        assertEquals(JobStatus.CLOSED, response.status());
        assertFalse(response.acceptingApplications());
    }

    @Test
    void searchOnlyUsesAvailableMatchingSpecificationAndRequestedPage() {
        when(repository.findAll(
                ArgumentMatchers.<Specification<Job>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(job(14L, JobStatus.OPEN))));

        JobSearchRequest request = new JobSearchRequest();
        request.setKeywords("spring");
        request.setLocation("remote");

        var page = service.search(request);

        assertEquals(1, page.content().size());
        assertEquals(JobStatus.OPEN, page.content().getFirst().status());
        verify(repository).findAll(
                ArgumentMatchers.<Specification<Job>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void rejectsInvalidExperienceRange() {
        JobSearchRequest request = new JobSearchRequest();
        request.setMinExperienceYears(5);
        request.setMaxExperienceYears(2);
        assertThrows(InvalidJobSearchException.class, () -> service.search(request));
    }

    @Test
    void missingJobIsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(JobNotFoundException.class, () -> service.getById(99L));
    }

    private Job job(Long id, JobStatus status) {
        Job job = new Job();
        job.setId(id);
        job.setTitle("Engineer");
        job.setDescription("description");
        job.setRequirements("requirements");
        job.setLocation("location");
        job.setEmploymentType("FULL_TIME");
        job.setExperienceYears(2);
        job.setStatus(status);
        job.setSkills(new java.util.LinkedHashSet<>(List.of("Java")));
        return job;
    }
}
