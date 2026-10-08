package com.example.people.integration;

import com.example.people.repository.PersonRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação completa (segurança, Flyway, JPA) sobre H2 e uma Nationalize falsa (WireMock).
 * O servidor WireMock é único para toda a suíte, para que o contexto Spring possa ser reaproveitado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    protected static final WireMockServer NATIONALIZE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        NATIONALIZE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(NATIONALIZE::stop));
    }

    @DynamicPropertySource
    static void nationalizeProperties(DynamicPropertyRegistry registry) {
        registry.add("app.nationalize.base-url", NATIONALIZE::baseUrl);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PersonRepository personRepository;

    @BeforeEach
    void resetState() {
        personRepository.deleteAll();
        NATIONALIZE.resetAll();
    }

    protected String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new Credentials(username, password));
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    protected String adminToken() throws Exception {
        return "Bearer " + login("admin", "admin123");
    }

    protected JsonNode json(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    private record Credentials(String username, String password) {
    }
}
