package com.revhire.job.service;

import com.revhire.job.dto.CreateJobRequest;
import com.revhire.job.dto.JobMapper;
import com.revhire.job.dto.JobResponse;
import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.dto.PageResponse;
import com.revhire.job.dto.UpdateJobRequest;
import com.revhire.job.entity.Job;
import com.revhire.job.entity.JobStatus;
import com.revhire.job.exception.InvalidJobSearchException;
import com.revhire.job.exception.JobNotFoundException;
import com.revhire.job.repository.JobRepository;
import com.revhire.job.repository.JobSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class JobService {
    private final JobRepository repository;
    private final JobMapper mapper;

    public JobService(JobRepository repository, JobMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public JobResponse create(CreateJobRequest request, Long recruiterId) {
        Job job = mapper.toEntity(request);
        job.setRecruiterId(recruiterId);
        return mapper.toResponse(repository.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getRecruiterJobs(Long recruiterId) {
        return repository.findAllByRecruiterId(recruiterId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getById(Long id) {
        return mapper.toResponse(findJob(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(JobSearchRequest request) {
        if (request.getMinExperienceYears() != null
                && request.getMaxExperienceYears() != null
                && request.getMinExperienceYears() > request.getMaxExperienceYears()) {
            throw new InvalidJobSearchException(
                    "minExperienceYears must not exceed maxExperienceYears");
        }
        Sort sort = Sort.by(
                Sort.Direction.valueOf(request.getDirection().name()),
                request.getSortBy().property()
        );
        Page<Job> result = repository.findAll(
                JobSpecifications.availableMatching(request),
                PageRequest.of(request.getPage(), request.getSize(), sort)
        );
        return PageResponse.from(result.map(mapper::toResponse));
    }

    public JobResponse update(Long id, Long recruiterId, UpdateJobRequest request) {
        Job job = findJob(id, recruiterId);
        mapper.updateEntity(job, request);
        return mapper.toResponse(repository.save(job));
    }

    public JobResponse close(Long id, Long recruiterId) {
        Job job = findJob(id, recruiterId);
        job.setStatus(JobStatus.CLOSED);
        return mapper.toResponse(repository.save(job));
    }

    private Job findJob(Long id, Long recruiterId) {
        return repository.findByIdAndRecruiterId(id, recruiterId)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    private Job findJob(Long id) {
        return repository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }
}
