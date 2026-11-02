package com.revhire.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record EducationRequest(
        @NotBlank @Size(max = 150) String institution,
        @NotBlank @Size(max = 120) String degree,
        @Size(max = 120) String fieldOfStudy,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
