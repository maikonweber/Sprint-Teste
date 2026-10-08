package com.example.people.integration;

import com.example.people.entity.Person;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NationalityIntegrationTest extends AbstractIntegrationTest {

    private Long savePerson(String name) {
        return personRepository.save(new Person("12345678900", name, "Silva", "person@example.com")).getId();
    }

    @Test
    void shouldReturnMostProbableNationality() throws Exception {
        Long id = savePerson("Nathaniel");
        NATIONALIZE.stubFor(WireMock.get(urlPathEqualTo("/")).withQueryParam("name", equalTo("Nathaniel"))
                .willReturn(okJson("""
                        {"count": 5000, "name": "Nathaniel", "country": [
                          {"country_id": "GB", "probability": 0.12},
                          {"country_id": "US", "probability": 0.42}]}
                        """)));

        mockMvc.perform(get("/findNacionalityByPerson/" + id).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personId").value(id))
                .andExpect(jsonPath("$.name").value("Nathaniel"))
                .andExpect(jsonPath("$.nationality").value("United States"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.probability").value(0.42));

        NATIONALIZE.verify(1, getRequestedFor(urlPathEqualTo("/")).withQueryParam("name", equalTo("Nathaniel")));
    }

    @Test
    void shouldReturnNotFoundWithoutCallingExternalApi() throws Exception {
        mockMvc.perform(get("/findNacionalityByPerson/999").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isNotFound());

        NATIONALIZE.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void shouldValidateId() throws Exception {
        mockMvc.perform(get("/findNacionalityByPerson/0").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/findNacionalityByPerson/xyz").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnServiceUnavailableWhenNationalizeFails() throws Exception {
        Long id = savePerson("Nathaniel");
        NATIONALIZE.stubFor(WireMock.get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(503)));

        mockMvc.perform(get("/findNacionalityByPerson/" + id).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Serviço de previsão de nacionalidade indisponível no momento"));
    }
}
