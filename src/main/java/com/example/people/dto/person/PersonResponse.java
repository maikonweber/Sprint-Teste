package com.example.people.dto.person;

import com.example.people.entity.Person;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pessoa cadastrada")
public record PersonResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "12345678900") String document,
        @Schema(example = "Nathaniel") String name,
        @Schema(example = "Silva") String surname,
        @Schema(example = "nathaniel@example.com") String email,
        Instant createdAt,
        Instant updatedAt) {

    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getDocument(),
                person.getName(),
                person.getSurname(),
                person.getEmail(),
                person.getCreatedAt(),
                person.getUpdatedAt());
    }
}
