package com.example.people.controller;

import com.example.people.dto.error.ErrorResponse;
import com.example.people.dto.person.PersonRequest;
import com.example.people.dto.person.PersonResponse;
import com.example.people.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Tag(name = "Pessoas", description = "Cadastro, consulta e exclusão de pessoas")
@ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class PersonController {

    static final String INVALID_ID_MESSAGE = "O id deve ser um número inteiro positivo";

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping("/registrarName")
    @Operation(summary = "Cadastra uma pessoa")
    @ApiResponse(responseCode = "201", description = "Pessoa cadastrada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Documento ou e-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PersonResponse> register(@Valid @RequestBody PersonRequest request) {
        PersonResponse created = personService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/list/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/list")
    @Operation(summary = "Lista todas as pessoas cadastradas")
    @ApiResponse(responseCode = "200", description = "Lista de pessoas (ordenada por id)")
    public List<PersonResponse> list() {
        return personService.findAll();
    }

    @GetMapping("/list/{id}")
    @Operation(summary = "Consulta uma pessoa pelo id")
    @ApiResponse(responseCode = "200", description = "Pessoa encontrada")
    @ApiResponse(responseCode = "400", description = "Id inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PersonResponse findById(
            @Parameter(description = "Id da pessoa (inteiro positivo)", example = "1")
            @PathVariable @Positive(message = INVALID_ID_MESSAGE) Long id) {
        return personService.findById(id);
    }

    @DeleteMapping("/list/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma pessoa pelo id")
    @ApiResponse(responseCode = "204", description = "Pessoa excluída")
    @ApiResponse(responseCode = "400", description = "Id inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(
            @Parameter(description = "Id da pessoa (inteiro positivo)", example = "1")
            @PathVariable @Positive(message = INVALID_ID_MESSAGE) Long id) {
        personService.delete(id);
    }
}
