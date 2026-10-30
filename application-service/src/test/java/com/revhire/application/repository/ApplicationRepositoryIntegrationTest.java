package com.revhire.application.repository;

import com.revhire.application.entity.Application;
import com.revhire.application.entity.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ApplicationRepositoryIntegrationTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsApplicationStatusAsString() {
        Application saved = applicationRepository.saveAndFlush(
                new Application(41L, 101L, ApplicationStatus.PENDING)
        );

        String storedStatus = jdbcTemplate.queryForObject(
                "select status from applications where application_id = ?",
                String.class,
                saved.getApplicationId()
        );
        Application retrieved = applicationRepository.findById(saved.getApplicationId()).orElseThrow();

        assertEquals("PENDING", storedStatus);
        assertEquals(ApplicationStatus.PENDING, retrieved.getStatus());
    }

    @Test
    void aggregatesAllCandidateApplicationsAndSupportedStatusCounts() {
        save(41L, 101L, ApplicationStatus.PENDING);
        save(41L, 102L, ApplicationStatus.REVIEWING);
        save(41L, 103L, ApplicationStatus.PENDING);
        save(42L, 104L, ApplicationStatus.REVIEWING);

        ApplicationRepository.ApplicationSummaryCounts summary =
                applicationRepository.getSummaryByCandidateId(41L);
        ApplicationRepository.ApplicationSummaryCounts emptySummary =
                applicationRepository.getSummaryByCandidateId(43L);

        assertEquals(3L, summary.getTotalApplications());
        assertEquals(2L, summary.getPendingApplications());
        assertEquals(1L, summary.getReviewingApplications());
        assertEquals(0L, emptySummary.getTotalApplications());
        assertEquals(0L, emptySummary.getPendingApplications());
        assertEquals(0L, emptySummary.getReviewingApplications());
    }

    @Test
    void paginatesAndFiltersWithinCandidate() {
        save(41L, 101L, ApplicationStatus.PENDING);
        save(41L, 102L, ApplicationStatus.REVIEWING);
        save(41L, 103L, ApplicationStatus.PENDING);
        save(42L, 104L, ApplicationStatus.PENDING);

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "appliedDate");
        Page<Application> firstPage = applicationRepository.findAllByCandidateId(
                41L, PageRequest.of(0, 2, newestFirst)
        );
        Page<Application> secondPage = applicationRepository.findAllByCandidateId(
                41L, PageRequest.of(1, 2, newestFirst)
        );
        Page<Application> pendingPage = applicationRepository.findAllByCandidateIdAndStatus(
                41L, ApplicationStatus.PENDING, PageRequest.of(0, 10, newestFirst)
        );
        Page<Application> reviewingPage = applicationRepository.findAllByCandidateIdAndStatus(
                41L, ApplicationStatus.REVIEWING, PageRequest.of(0, 10, newestFirst)
        );

        assertEquals(3L, firstPage.getTotalElements());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(3L, secondPage.getTotalElements());
        assertEquals(1, secondPage.getContent().size());
        assertTrue(firstPage.getContent().stream()
                .allMatch(application -> application.getCandidateId().equals(41L)));
        assertTrue(secondPage.getContent().stream()
                .allMatch(application -> application.getCandidateId().equals(41L)));
        Set<Long> firstPageIds = firstPage.getContent().stream()
                .map(Application::getApplicationId)
                .collect(Collectors.toSet());
        Set<Long> secondPageIds = secondPage.getContent().stream()
                .map(Application::getApplicationId)
                .collect(Collectors.toSet());
        assertTrue(Collections.disjoint(firstPageIds, secondPageIds));
        Set<Long> pagedApplicationIds = new HashSet<>(firstPageIds);
        pagedApplicationIds.addAll(secondPageIds);
        assertEquals(3, pagedApplicationIds.size());
        assertEquals(2L, pendingPage.getTotalElements());
        assertEquals(1L, reviewingPage.getTotalElements());
        assertEquals(41L, firstPage.getContent().getFirst().getCandidateId());
        assertEquals(41L, secondPage.getContent().getFirst().getCandidateId());
        assertEquals(41L, pendingPage.getContent().getFirst().getCandidateId());
        assertEquals(ApplicationStatus.PENDING, pendingPage.getContent().getFirst().getStatus());
        assertEquals(41L, reviewingPage.getContent().getFirst().getCandidateId());
        assertEquals(ApplicationStatus.REVIEWING, reviewingPage.getContent().getFirst().getStatus());
    }

    private void save(Long candidateId, Long jobId, ApplicationStatus status) {
        applicationRepository.save(new Application(candidateId, jobId, status));
    }
}
