package com.revhire.profile.repository;

import com.revhire.profile.entity.RecruiterProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
    Optional<RecruiterProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
