package com.revhire.profile.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "candidate_profile_user_uq", columnNames = "user_id")
})
public class CandidateProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String location;

    @Column(length = 160)
    private String headline;

    @Column(length = 2000)
    private String summary;

    @Column(length = 500)
    private String linkedinUrl;

    @Column(length = 500)
    private String portfolioUrl;

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> education = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experience = new ArrayList<>();

    @OneToOne(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Resume resume;

    protected CandidateProfile() {
    }

    public CandidateProfile(Long userId, String firstName, String lastName, String phone, String location,
                            String headline, String summary, String linkedinUrl, String portfolioUrl) {
        this.userId = userId;
        update(firstName, lastName, phone, location, headline, summary, linkedinUrl, portfolioUrl);
    }

    public void update(String firstName, String lastName, String phone, String location, String headline,
                       String summary, String linkedinUrl, String portfolioUrl) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.location = location;
        this.headline = headline;
        this.summary = summary;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public String getHeadline() { return headline; }
    public String getSummary() { return summary; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
}
