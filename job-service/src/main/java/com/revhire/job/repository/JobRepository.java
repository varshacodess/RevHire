package com.revhire.job.repository;

import com.revhire.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    List<Job> findAllByRecruiterId(Long recruiterId);

    Optional<Job> findByIdAndRecruiterId(Long id, Long recruiterId);
}
