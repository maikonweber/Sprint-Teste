package com.example.people.dto.nationality;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nacionalidade mais provável de uma pessoa, segundo a API Nationalize")
public record NationalityResponse(
        @Schema(example = "1") Long personId,
        @Schema(example = "Nathaniel") String name,
        @Schema(description = "Nome do país em inglês, ou \"Unknown\" quando não há previsão/código desconhecido",
                example = "United States")
        String nationality,
        @Schema(description = "Código ISO 3166-1 alpha-2 (nulo quando não há previsão)", example = "US")
        String countryCode,
        @Schema(description = "Probabilidade entre 0 e 1 (nula quando não há previsão)", example = "0.42")
        Double probability) {
}
