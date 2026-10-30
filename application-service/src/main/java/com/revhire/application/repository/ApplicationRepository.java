package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    List<Application> findAllByCandidateIdOrderByAppliedDateDesc(Long candidateId);

    Page<Application> findAllByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findAllByCandidateIdAndStatus(Long candidateId, String status, Pageable pageable);

    Optional<Application> findByApplicationIdAndCandidateId(Long applicationId, Long candidateId);
}
