package com.example.people.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param secret chave HMAC-SHA256; precisa de pelo menos 256 bits (32 bytes).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET deve ser configurado")
        @Size(min = 32, message = "JWT_SECRET deve ter pelo menos 32 caracteres")
        String secret,
        @NotNull Duration expiration,
        @NotBlank String issuer) {
}
