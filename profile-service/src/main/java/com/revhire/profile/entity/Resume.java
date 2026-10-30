package com.revhire.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidate_profile_resumes")
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false, unique = true)
    private CandidateProfile candidateProfile;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(length = 100)
    private String contentType;

    protected Resume() {
    }

    public Resume(CandidateProfile candidateProfile, String fileName, String fileUrl, String contentType) {
        this.candidateProfile = candidateProfile;
        update(fileName, fileUrl, contentType);
    }

    public void update(String fileName, String fileUrl, String contentType) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public String getContentType() { return contentType; }
}
