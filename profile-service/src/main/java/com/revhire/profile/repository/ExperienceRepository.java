package com.revhire.profile.repository;

import com.revhire.profile.entity.Experience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findAllByCandidateProfileUserId(Long userId);

    Optional<Experience> findByIdAndCandidateProfileUserId(Long id, Long userId);
}
