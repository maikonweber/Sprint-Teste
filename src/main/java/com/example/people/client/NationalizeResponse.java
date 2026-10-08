package com.example.people.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resposta de {@code GET https://api.nationalize.io/?name=...}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NationalizeResponse(Long count, String name, List<CountryProbability> country) {

    public Optional<CountryProbability> mostProbableCountry() {
        if (country == null) {
            return Optional.empty();
        }
        return country.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.countryId() != null && candidate.probability() != null)
                .max(Comparator.comparingDouble(CountryProbability::probability));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CountryProbability(@JsonProperty("country_id") String countryId, Double probability) {
    }
}
