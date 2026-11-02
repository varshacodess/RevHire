package com.revhire.profile.repository;

import com.revhire.profile.entity.Education;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findAllByCandidateProfileUserId(Long userId);

    Optional<Education> findByIdAndCandidateProfileUserId(Long id, Long userId);
}
