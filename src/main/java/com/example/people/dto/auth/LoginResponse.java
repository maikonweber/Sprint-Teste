package com.example.people.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso JWT")
public record LoginResponse(
        @Schema(description = "JWT a ser enviado no header Authorization: Bearer <token>") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Validade do token em segundos", example = "3600") long expiresIn) {
}
