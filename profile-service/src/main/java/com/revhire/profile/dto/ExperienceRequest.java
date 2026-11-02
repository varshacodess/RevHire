package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record ExperienceRequest(
        @NotBlank @Size(max = 150) String companyName,
        @NotBlank @Size(max = 120) String jobTitle,
        @Size(max = 120) String location,
        @Size(max = 2000) String description,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
