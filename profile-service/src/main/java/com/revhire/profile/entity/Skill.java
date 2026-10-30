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

@Entity
@Table(name = "candidate_profile_skills")
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String proficiency;

    protected Skill() {
    }

    public Skill(CandidateProfile candidateProfile, String name, String proficiency) {
        this.candidateProfile = candidateProfile;
        update(name, proficiency);
    }

    public void update(String name, String proficiency) {
        this.name = name;
        this.proficiency = proficiency;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProficiency() { return proficiency; }
}
