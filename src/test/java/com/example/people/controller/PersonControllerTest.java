package com.example.people.controller;

import com.example.people.config.PropertiesConfig;
import com.example.people.config.SecurityConfig;
import com.example.people.dto.person.PersonRequest;
import com.example.people.dto.person.PersonResponse;
import com.example.people.exception.DuplicateResourceException;
import com.example.people.exception.ResourceNotFoundException;
import com.example.people.security.CustomUserDetailsService;
import com.example.people.security.JwtService;
import com.example.people.security.SecurityErrorHandler;
import com.example.people.service.PersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonController.class)
@Import({SecurityConfig.class, PropertiesConfig.class, SecurityErrorHandler.class})
@ActiveProfiles("test")
@WithMockUser
class PersonControllerTest {

    private static final PersonResponse PERSON = new PersonResponse(
            1L, "12345678900", "Nathaniel", "Silva", "nathaniel@example.com", Instant.now(), Instant.now());

    private static final String VALID_BODY = """
            {"document": "12345678900", "name": "Nathaniel", "surname": "Silva", "email": "nathaniel@example.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonService personService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldRegisterValidPerson() throws Exception {
        when(personService.create(any(PersonRequest.class))).thenReturn(PERSON);

        mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/list/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("nathaniel@example.com"));
    }

    @Test
    void shouldRejectInvalidPayloadWithFieldErrors() throws Exception {
        String body = """
                {"document": "123", "name": "N", "surname": "", "email": "not-an-email"}
                """;

        mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/registrarName"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("document")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("surname")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("E-mail inválido")));
        verifyNoInteractions(personService);
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou malformado"));
    }

    @Test
    void shouldReturnConflictForDuplicates() throws Exception {
        when(personService.create(any(PersonRequest.class)))
                .thenThrow(new DuplicateResourceException("Já existe uma pessoa cadastrada com este e-mail"));

        mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Já existe uma pessoa cadastrada com este e-mail"));
    }

    @Test
    void shouldListPeople() throws Exception {
        when(personService.findAll()).thenReturn(List.of(PERSON));

        mockMvc.perform(get("/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].document").value("12345678900"));
    }

    @Test
    void shouldFindPersonById() throws Exception {
        when(personService.findById(1L)).thenReturn(PERSON);

        mockMvc.perform(get("/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nathaniel"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "99999999999999999999"})
    void shouldRejectInvalidIds(String id) throws Exception {
        mockMvc.perform(get("/list/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(personService);
    }

    @Test
    void shouldReturnNotFoundForMissingPerson() throws Exception {
        when(personService.findById(99L)).thenThrow(ResourceNotFoundException.person(99L));

        mockMvc.perform(get("/list/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pessoa com id 99 não encontrada"))
                .andExpect(jsonPath("$.path").value("/list/99"));
    }

    @Test
    void shouldDeletePerson() throws Exception {
        mockMvc.perform(delete("/list/1")).andExpect(status().isNoContent());
        verify(personService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingPerson() throws Exception {
        doThrow(ResourceNotFoundException.person(99L)).when(personService).delete(99L);

        mockMvc.perform(delete("/list/99")).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidIdOnDelete() throws Exception {
        mockMvc.perform(delete("/list/-3")).andExpect(status().isBadRequest());
        verifyNoInteractions(personService);
    }

    @Test
    void shouldNotExposeInternalErrorDetails() throws Exception {
        when(personService.findAll()).thenThrow(new IllegalStateException("connection string with secrets"));

        mockMvc.perform(get("/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno inesperado"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
