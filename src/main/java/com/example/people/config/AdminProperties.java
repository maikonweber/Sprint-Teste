package com.example.people.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank(message = "ADMIN_USERNAME deve ser configurado") String username,
        @NotBlank(message = "ADMIN_PASSWORD deve ser configurado")
        @Size(min = 8, message = "ADMIN_PASSWORD deve ter pelo menos 8 caracteres")
        String password) {
}
