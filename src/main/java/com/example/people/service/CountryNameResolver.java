package com.example.people.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Converte códigos ISO 3166-1 alpha-2 (ex.: BR, US, GB) em nomes de países em inglês,
 * usando os dados de localização da própria JDK. Códigos ausentes ou desconhecidos
 * resultam em {@value #UNKNOWN}, nunca em exceção.
 */
@Component
public class CountryNameResolver {

    public static final String UNKNOWN = "Unknown";

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public String resolve(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) {
            return UNKNOWN;
        }
        String normalized = isoCode.trim().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(normalized)) {
            return UNKNOWN;
        }
        return Locale.of("", normalized).getDisplayCountry(Locale.ENGLISH);
    }
}
