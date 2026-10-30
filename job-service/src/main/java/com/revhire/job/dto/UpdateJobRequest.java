package com.revhire.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateJobRequest(
        @Size(max = 150) @Pattern(regexp = ".*\\S.*") String title,
        @Pattern(regexp = ".*\\S.*") String description,
        @Pattern(regexp = ".*\\S.*") String requirements,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*") String location,
        @Size(max = 50) @Pattern(regexp = ".*\\S.*") String employmentType,
        @Min(0) Integer experienceYears,
        List<@NotBlank @Size(max = 100) String> skills
) {
}
