package com.revhire.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class JwtSecurityConfiguration {

    private static final Set<String> ALLOWED_ROLES = Set.of("CANDIDATE", "RECRUITER");

    @Bean
    JwtDecoder jwtDecoder(@Value("${JWT_SECRET}") String encodedSecret) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", exception);
        }

        int keyLengthBits = Math.multiplyExact(secret.length, Byte.SIZE);
        if (keyLengthBits < 256) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 256 bits");
        }

        MacAlgorithm macAlgorithm;
        String keyAlgorithm;
        if (keyLengthBits < 384) {
            macAlgorithm = MacAlgorithm.HS256;
            keyAlgorithm = "HmacSHA256";
        } else if (keyLengthBits < 512) {
            macAlgorithm = MacAlgorithm.HS384;
            keyAlgorithm = "HmacSHA384";
        } else {
            macAlgorithm = MacAlgorithm.HS512;
            keyAlgorithm = "HmacSHA512";
        }

        SecretKey key = new SecretKeySpec(secret, keyAlgorithm);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(macAlgorithm)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                requiredClaimsValidator()
        ));
        return decoder;
    }

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/applications/**").hasRole("CANDIDATE")
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            String subject = jwt.getSubject();
            Object role = jwt.getClaims().get("role");
            if (subject == null || subject.isBlank()) {
                return invalidToken("JWT subject is required");
            }
            if (!(role instanceof String roleValue) || !ALLOWED_ROLES.contains(roleValue)) {
                return invalidToken("JWT role is missing or invalid");
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null)
        );
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of(
                new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString("role"))
        ));
        return converter;
    }
}
