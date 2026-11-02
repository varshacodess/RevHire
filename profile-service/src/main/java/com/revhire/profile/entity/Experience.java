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
@Table(name = "candidate_experience")
public class Experience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 120)
    private String jobTitle;

    @Column(length = 120)
    private String location;

    @Column(length = 2000)
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    protected Experience() {
    }

    public Experience(CandidateProfile candidateProfile, String companyName, String jobTitle, String location,
                      String description, LocalDate startDate, LocalDate endDate) {
        this.candidateProfile = candidateProfile;
        update(companyName, jobTitle, location, description, startDate, endDate);
    }

    public void update(String companyName, String jobTitle, String location, String description,
                       LocalDate startDate, LocalDate endDate) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getJobTitle() { return jobTitle; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
