package com.example.people.service;

import com.example.people.client.NationalizeClient;
import com.example.people.client.NationalizeResponse;
import com.example.people.client.NationalizeResponse.CountryProbability;
import com.example.people.dto.nationality.NationalityResponse;
import com.example.people.dto.person.PersonResponse;
import com.example.people.exception.ExternalServiceException;
import com.example.people.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NationalityServiceTest {

    private static final PersonResponse PERSON = new PersonResponse(
            1L, "12345678900", "Nathaniel", "Silva", "nathaniel@example.com", Instant.now(), Instant.now());

    @Mock
    private PersonService personService;

    @Mock
    private NationalizeClient nationalizeClient;

    private NationalityService nationalityService;

    @BeforeEach
    void setUp() {
        nationalityService = new NationalityService(personService, nationalizeClient, new CountryNameResolver());
    }

    @Test
    void shouldReturnCountryWithHighestProbability() {
        when(personService.findById(1L)).thenReturn(PERSON);
        when(nationalizeClient.predictNationality("Nathaniel")).thenReturn(new NationalizeResponse(100L, "nathaniel", List.of(
                new CountryProbability("GB", 0.11),
                new CountryProbability("US", 0.42),
                new CountryProbability("BR", 0.07))));

        NationalityResponse response = nationalityService.findByPersonId(1L);

        assertThat(response).isEqualTo(new NationalityResponse(1L, "Nathaniel", "United States", "US", 0.42));
    }

    @Test
    void shouldReturnUnknownNationalityForUnrecognizedIsoCode() {
        when(personService.findById(1L)).thenReturn(PERSON);
        when(nationalizeClient.predictNationality("Nathaniel"))
                .thenReturn(new NationalizeResponse(1L, "nathaniel", List.of(new CountryProbability("zz", 0.9))));

        NationalityResponse response = nationalityService.findByPersonId(1L);

        assertThat(response.nationality()).isEqualTo(CountryNameResolver.UNKNOWN);
        assertThat(response.countryCode()).isEqualTo("ZZ");
        assertThat(response.probability()).isEqualTo(0.9);
    }

    @Test
    void shouldReturnUnknownWhenApiHasNoPrediction() {
        when(personService.findById(1L)).thenReturn(PERSON);
        when(nationalizeClient.predictNationality("Nathaniel"))
                .thenReturn(new NationalizeResponse(0L, "nathaniel", List.of()));

        NationalityResponse response = nationalityService.findByPersonId(1L);

        assertThat(response).isEqualTo(new NationalityResponse(1L, "Nathaniel", CountryNameResolver.UNKNOWN, null, null));
    }

    @Test
    void shouldNotCallExternalApiWhenPersonDoesNotExist() {
        when(personService.findById(99L)).thenThrow(ResourceNotFoundException.person(99L));

        assertThatThrownBy(() -> nationalityService.findByPersonId(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(nationalizeClient, never()).predictNationality(anyString());
    }

    @Test
    void shouldPropagateExternalServiceFailure() {
        when(personService.findById(1L)).thenReturn(PERSON);
        when(nationalizeClient.predictNationality("Nathaniel"))
                .thenThrow(new ExternalServiceException("indisponível", null));

        assertThatThrownBy(() -> nationalityService.findByPersonId(1L)).isInstanceOf(ExternalServiceException.class);
    }
}
