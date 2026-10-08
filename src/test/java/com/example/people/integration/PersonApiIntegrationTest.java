package com.example.people.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersonApiIntegrationTest extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void authenticate() throws Exception {
        token = adminToken();
    }

    private MockHttpServletRequestBuilder register(String document, String email) {
        String body = """
                {"document": "%s", "name": "Nathaniel", "surname": "Silva", "email": "%s"}
                """.formatted(document, email);
        return post("/registrarName")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void shouldCreateListFindAndDeletePerson() throws Exception {
        String created = mockMvc.perform(register("12345678900", "nathaniel@example.com"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json(created).get("id").asLong();

        mockMvc.perform(get("/list").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].document").value("12345678900"))
                .andExpect(jsonPath("$[0].name").value("Nathaniel"))
                .andExpect(jsonPath("$[0].surname").value("Silva"))
                .andExpect(jsonPath("$[0].email").value("nathaniel@example.com"));

        mockMvc.perform(get("/list/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(delete("/list/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/list/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/list/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicateDocument() throws Exception {
        mockMvc.perform(register("12345678900", "first@example.com")).andExpect(status().isCreated());

        mockMvc.perform(register("12345678900", "second@example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe uma pessoa cadastrada com este documento"));
    }

    @Test
    void shouldRejectDuplicateEmailCaseInsensitively() throws Exception {
        mockMvc.perform(register("12345678900", "same@example.com")).andExpect(status().isCreated());

        mockMvc.perform(register("98765432100", "SAME@Example.com"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe uma pessoa cadastrada com este e-mail"));
    }

    @Test
    void shouldValidatePayload() throws Exception {
        mockMvc.perform(register("abc", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void shouldValidateIdParameter() throws Exception {
        mockMvc.perform(get("/list/abc").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/list/0").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O id deve ser um número inteiro positivo"));
        mockMvc.perform(delete("/list/-1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowCorsPreflightOnlyFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/list")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/list")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
