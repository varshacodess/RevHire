package com.revhire.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateJobRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @NotBlank String requirements,
        @NotBlank @Size(max = 100) String location,
        @NotBlank @Size(max = 50) String employmentType,
        @NotNull @Min(0) Integer experienceYears,
        List<@NotBlank @Size(max = 100) String> skills
) {
}
