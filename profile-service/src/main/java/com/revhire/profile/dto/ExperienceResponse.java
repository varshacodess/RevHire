package com.revhire.profile.dto;

import java.time.LocalDate;

public record ExperienceResponse(
        Long id,
        String companyName,
        String jobTitle,
        String location,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
