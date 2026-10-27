package com.revhire.authentication.dto;

public record CandidateResponse(
        Long id,
        String fullName,
        String email,
        String username
) {
}
