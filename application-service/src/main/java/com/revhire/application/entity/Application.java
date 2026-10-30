package com.revhire.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_candidate_job",
                columnNames = {"candidate_id", "job_id"}
        )
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "applied_date", nullable = false)
    private LocalDateTime appliedDate;

    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    protected Application() {
    }

    public Application(Long candidateId, Long jobId, ApplicationStatus status) {
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.status = status;
    }

    @PrePersist
    void setCreatedDates() {
        LocalDateTime now = LocalDateTime.now();
        appliedDate = now;
        updatedDate = now;
    }

    @PreUpdate
    void setUpdatedDate() {
        updatedDate = LocalDateTime.now();
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public Long getJobId() {
        return jobId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
}
