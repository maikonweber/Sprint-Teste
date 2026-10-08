package com.example.people.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.nationalize")
public record NationalizeProperties(@NotBlank String baseUrl, @NotNull Duration timeout) {
}
