package com.example.people.service;

import com.example.people.client.NationalizeClient;
import com.example.people.client.NationalizeResponse.CountryProbability;
import com.example.people.dto.nationality.NationalityResponse;
import com.example.people.dto.person.PersonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class NationalityService {

    private static final Logger log = LoggerFactory.getLogger(NationalityService.class);

    private final PersonService personService;
    private final NationalizeClient nationalizeClient;
    private final CountryNameResolver countryNameResolver;

    public NationalityService(PersonService personService, NationalizeClient nationalizeClient,
                              CountryNameResolver countryNameResolver) {
        this.personService = personService;
        this.nationalizeClient = nationalizeClient;
        this.countryNameResolver = countryNameResolver;
    }

    /**
     * Prevê a nacionalidade mais provável da pessoa a partir do seu nome.
     * Quando a Nationalize não tem previsão para o nome, retorna nacionalidade {@value CountryNameResolver#UNKNOWN}.
     */
    public NationalityResponse findByPersonId(Long personId) {
        PersonResponse person = personService.findById(personId);

        return nationalizeClient.predictNationality(person.name())
                .mostProbableCountry()
                .map(country -> toResponse(person, country))
                .orElseGet(() -> {
                    log.info("Nationalize sem previsão para a pessoa id={}", personId);
                    return new NationalityResponse(person.id(), person.name(), CountryNameResolver.UNKNOWN, null, null);
                });
    }

    private NationalityResponse toResponse(PersonResponse person, CountryProbability country) {
        String countryCode = country.countryId().trim().toUpperCase(Locale.ROOT);
        return new NationalityResponse(
                person.id(),
                person.name(),
                countryNameResolver.resolve(countryCode),
                countryCode,
                country.probability());
    }
}
