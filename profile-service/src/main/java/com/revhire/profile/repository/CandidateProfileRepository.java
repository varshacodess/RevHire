package com.revhire.profile.repository;

import com.revhire.profile.entity.CandidateProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
