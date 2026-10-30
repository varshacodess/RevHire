package com.revhire.profile.repository;

import com.revhire.profile.entity.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByCandidateProfileUserId(Long userId);

    Optional<Skill> findByIdAndCandidateProfileUserId(Long id, Long userId);
}
