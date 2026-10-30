package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    List<Application> findAllByCandidateIdOrderByAppliedDateDesc(Long candidateId);

    Optional<Application> findByApplicationIdAndCandidateId(Long applicationId, Long candidateId);
}
