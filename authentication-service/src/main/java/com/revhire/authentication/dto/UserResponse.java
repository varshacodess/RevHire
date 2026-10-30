package com.revhire.authentication.dto;

import com.revhire.authentication.entity.Role;

public record UserResponse(Long id, String fullName, String email, String username, Role role) {
}
