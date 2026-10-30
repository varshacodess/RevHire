package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    List<Application> findAllByCandidateIdOrderByAppliedDateDesc(Long candidateId);

    Page<Application> findAllByCandidateId(Long candidateId, Pageable pageable);

    Page<Application> findAllByCandidateIdAndStatus(
            Long candidateId,
            ApplicationStatus status,
            Pageable pageable
    );

    Optional<Application> findByApplicationIdAndCandidateId(Long applicationId, Long candidateId);

    @Query("""
            select count(a) as totalApplications,
                   count(case when a.status = com.revhire.application.entity.ApplicationStatus.PENDING
                              then a.applicationId end) as pendingApplications,
                   count(case when a.status = com.revhire.application.entity.ApplicationStatus.REVIEWING
                              then a.applicationId end) as reviewingApplications
            from Application a
            where a.candidateId = :candidateId
            """)
    ApplicationSummaryCounts getSummaryByCandidateId(@Param("candidateId") Long candidateId);

    interface ApplicationSummaryCounts {
        Long getTotalApplications();

        Long getPendingApplications();

        Long getReviewingApplications();
    }
}
