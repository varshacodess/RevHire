package com.revhire.authentication.mapper;

import com.revhire.authentication.dto.UserResponse;
import com.revhire.authentication.entity.Role;
import com.revhire.authentication.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(String fullName, String email, String username, String passwordHash, Role role) {
        return new User(fullName, email, username, passwordHash, role);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getUsername(), user.getRole());
    }
}
