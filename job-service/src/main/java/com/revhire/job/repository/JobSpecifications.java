package com.revhire.job.repository;

import com.revhire.job.dto.JobSearchRequest;
import com.revhire.job.entity.Job;
import com.revhire.job.entity.JobStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobSpecifications {
    private JobSpecifications() {
    }

    public static Specification<Job> availableMatching(JobSearchRequest request) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), JobStatus.OPEN));

            addContains(predicates, builder, root.get("title"), request.getTitle());
            addContains(predicates, builder, root.get("location"), request.getLocation());
            addContains(predicates, builder, root.get("employmentType"), request.getEmploymentType());

            if (hasText(request.getKeywords())) {
                String pattern = containsPattern(request.getKeywords());
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern, '\\'),
                        builder.like(builder.lower(root.get("description")), pattern, '\\'),
                        builder.like(builder.lower(root.get("requirements")), pattern, '\\')
                ));
            }
            if (request.getMinExperienceYears() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("experienceYears"), request.getMinExperienceYears()));
            }
            if (request.getMaxExperienceYears() != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("experienceYears"), request.getMaxExperienceYears()));
            }
            if (hasText(request.getSkill())) {
                Join<Job, String> skills = root.join("skills", JoinType.INNER);
                predicates.add(builder.equal(
                        builder.lower(skills),
                        request.getSkill().trim().toLowerCase(Locale.ROOT)
                ));
                query.distinct(true);
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addContains(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.Path<String> path,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(builder.like(builder.lower(path), containsPattern(value), '\\'));
        }
    }

    private static String containsPattern(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
