package com.example.people.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(
        @Schema(example = "admin")
        @NotBlank(message = "Usuário é obrigatório")
        @Size(max = 50, message = "Usuário deve ter no máximo 50 caracteres")
        String username,

        @Schema(example = "admin123")
        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
