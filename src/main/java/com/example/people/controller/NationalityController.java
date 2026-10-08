package com.example.people.controller;

import com.example.people.dto.error.ErrorResponse;
import com.example.people.dto.nationality.NationalityResponse;
import com.example.people.service.NationalityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Nacionalidade", description = "Previsão de nacionalidade via Nationalize.io")
public class NationalityController {

    private final NationalityService nationalityService;

    public NationalityController(NationalityService nationalityService) {
        this.nationalityService = nationalityService;
    }

    @GetMapping("/findNacionalityByPerson/{id}")
    @Operation(summary = "Prevê a nacionalidade mais provável de uma pessoa a partir do seu nome")
    @ApiResponse(responseCode = "200", description = "Previsão realizada (nationality = \"Unknown\" quando não há previsão)")
    @ApiResponse(responseCode = "400", description = "Id inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "API Nationalize indisponível",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public NationalityResponse findByPerson(
            @Parameter(description = "Id da pessoa (inteiro positivo)", example = "1")
            @PathVariable @Positive(message = PersonController.INVALID_ID_MESSAGE) Long id) {
        return nationalityService.findByPersonId(id);
    }
}
