package com.revhire.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record CandidateLoginRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
