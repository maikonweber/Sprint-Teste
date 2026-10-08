package com.example.people.dto.person;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cadastro de uma pessoa")
public record PersonRequest(

        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos), apenas números", example = "12345678900")
        @NotBlank(message = "Documento é obrigatório")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "Documento deve conter 11 (CPF) ou 14 (CNPJ) dígitos numéricos")
        String document,

        @Schema(description = "Primeiro nome", example = "Nathaniel", minLength = 2, maxLength = 100)
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @Schema(description = "Sobrenome", example = "Silva", minLength = 2, maxLength = 100)
        @NotBlank(message = "Sobrenome é obrigatório")
        @Size(min = 2, max = 100, message = "Sobrenome deve ter entre 2 e 100 caracteres")
        String surname,

        @Schema(description = "E-mail único", example = "nathaniel@example.com", maxLength = 254)
        @NotBlank(message = "E-mail é obrigatório")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "E-mail inválido")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres")
        String email) {
}
