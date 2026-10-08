package com.example.people.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryNameResolverTest {

    private final CountryNameResolver resolver = new CountryNameResolver();

    @ParameterizedTest
    @CsvSource({
            "BR, Brazil",
            "US, United States",
            "GB, United Kingdom",
            "CA, Canada",
            "br, Brazil",
            "' de ', Germany"
    })
    void shouldResolveKnownIsoCodes(String code, String expectedName) {
        assertThat(resolver.resolve(code)).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "ZZ", "XX", "BRA", "1", "U"})
    void shouldReturnUnknownForMissingOrInvalidCodes(String code) {
        assertThat(resolver.resolve(code)).isEqualTo(CountryNameResolver.UNKNOWN);
    }
}
