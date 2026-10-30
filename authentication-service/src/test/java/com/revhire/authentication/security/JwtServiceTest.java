package com.revhire.authentication.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.revhire.authentication.entity.Role;
import com.revhire.authentication.entity.User;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {
    @Test
    void tokenContainsUserIdentityAndRole() {
        JwtService jwtService = new JwtService(Base64.getEncoder().encodeToString(new byte[32]), 60_000);
        User user = new User("Candidate", "candidate@example.com", "candidate", "hash", Role.CANDIDATE);
        ReflectionTestUtils.setField(user, "id", 42L);

        var claims = jwtService.parseToken(jwtService.generateToken(user)).getPayload();

        assertEquals("42", claims.getSubject());
        assertEquals("candidate@example.com", claims.get("email"));
        assertEquals("candidate", claims.get("username"));
        assertEquals("CANDIDATE", claims.get("role"));
    }

    @Test
    void tokenContainsRecruiterRole() {
        JwtService jwtService = new JwtService(Base64.getEncoder().encodeToString(new byte[32]), 60_000);
        User user = new User("Recruiter", "recruiter@example.com", "recruiter", "hash", Role.RECRUITER);
        ReflectionTestUtils.setField(user, "id", 43L);

        var claims = jwtService.parseToken(jwtService.generateToken(user)).getPayload();

        assertEquals("RECRUITER", claims.get("role"));
    }
}
