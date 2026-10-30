package com.revhire.profile.repository;

import com.revhire.profile.entity.Resume;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByCandidateProfileUserId(Long userId);
}
