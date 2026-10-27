package com.revhire.authentication.repository;

import com.revhire.authentication.entity.Candidate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<Candidate> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);
}
