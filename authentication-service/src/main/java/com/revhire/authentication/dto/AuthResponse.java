package com.revhire.authentication.dto;

public record AuthResponse(String token, String tokenType, UserResponse user) {
}
