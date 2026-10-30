package com.revhire.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recruiter_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "recruiter_profile_user_uq", columnNames = "user_id")
})
public class RecruiterProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String organizationName;

    @Column(name = "recruiter_name", nullable = false, length = 150)
    private String recruiterName;

    @Column(length = 120)
    private String jobTitle;

    @Column(length = 120)
    private String location;

    @Column(length = 2000)
    private String organizationDescription;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 30)
    private String phone;

    protected RecruiterProfile() {
    }

    public RecruiterProfile(Long userId, String recruiterName, String organizationName, String jobTitle,
                            String location, String organizationDescription, String websiteUrl, String phone) {
        this.userId = userId;
        update(recruiterName, organizationName, jobTitle, location, organizationDescription, websiteUrl, phone);
    }

    public void update(String recruiterName, String organizationName, String jobTitle, String location,
                       String organizationDescription, String websiteUrl, String phone) {
        this.recruiterName = recruiterName;
        this.organizationName = organizationName;
        this.jobTitle = jobTitle;
        this.location = location;
        this.organizationDescription = organizationDescription;
        this.websiteUrl = websiteUrl;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getRecruiterName() { return recruiterName; }
    public String getOrganizationName() { return organizationName; }
    public String getJobTitle() { return jobTitle; }
    public String getLocation() { return location; }
    public String getOrganizationDescription() { return organizationDescription; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getPhone() { return phone; }
}
