package com.revhire.profile.mapper;

import com.revhire.profile.dto.ResumeRequest;
import com.revhire.profile.dto.ResumeResponse;
import com.revhire.profile.entity.CandidateProfile;
import com.revhire.profile.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public Resume toEntity(CandidateProfile profile, ResumeRequest request) {
        return new Resume(profile, request.fileName().trim(), request.fileUrl().trim(), request.contentType());
    }

    public void update(Resume resume, ResumeRequest request) {
        resume.update(request.fileName().trim(), request.fileUrl().trim(), request.contentType());
    }

    public ResumeResponse toResponse(Resume resume) {
        return new ResumeResponse(resume.getId(), resume.getFileName(), resume.getFileUrl(), resume.getContentType());
    }
}
