package com.example.people.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-more-than-32-chars";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(5), "people-api"));

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken("admin");

        assertThat(jwtService.extractValidUsername(token)).contains("admin");
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken("admin");
        int position = token.lastIndexOf('.') + 5;
        char replacement = token.charAt(position) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, position) + replacement + token.substring(position + 1);

        assertThat(jwtService.extractValidUsername(tampered)).isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService expiredIssuer = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1), "people-api"));

        assertThat(jwtService.extractValidUsername(expiredIssuer.generateToken("admin"))).isEmpty();
    }

    @Test
    void shouldRejectTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(
                new JwtProperties("another-secret-key-with-more-than-32-chars!", Duration.ofMinutes(5), "people-api"));

        assertThat(jwtService.extractValidUsername(other.generateToken("admin"))).isEmpty();
    }

    @Test
    void shouldRejectTokenFromAnotherIssuer() {
        JwtService other = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(5), "someone-else"));

        assertThat(jwtService.extractValidUsername(other.generateToken("admin"))).isEmpty();
    }

    @Test
    void shouldRejectGarbage() {
        assertThat(jwtService.extractValidUsername("not-a-jwt")).isEmpty();
        assertThat(jwtService.extractValidUsername("")).isEmpty();
    }
}
