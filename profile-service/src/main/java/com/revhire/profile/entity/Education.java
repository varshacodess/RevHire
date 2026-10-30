package com.revhire.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "candidate_education")
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false, length = 150)
    private String institution;

    @Column(nullable = false, length = 120)
    private String degree;

    @Column(length = 120)
    private String fieldOfStudy;

    private LocalDate startDate;
    private LocalDate endDate;

    protected Education() {
    }

    public Education(CandidateProfile candidateProfile, String institution, String degree, String fieldOfStudy,
                     LocalDate startDate, LocalDate endDate) {
        this.candidateProfile = candidateProfile;
        update(institution, degree, fieldOfStudy, startDate, endDate);
    }

    public void update(String institution, String degree, String fieldOfStudy, LocalDate startDate, LocalDate endDate) {
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public String getInstitution() { return institution; }
    public String getDegree() { return degree; }
    public String getFieldOfStudy() { return fieldOfStudy; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
