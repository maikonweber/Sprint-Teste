package com.example.people.service;

import com.example.people.dto.person.PersonRequest;
import com.example.people.dto.person.PersonResponse;
import com.example.people.entity.Person;
import com.example.people.exception.DuplicateResourceException;
import com.example.people.exception.ResourceNotFoundException;
import com.example.people.repository.PersonRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private PersonService personService;

    private static Person person(Long id, String document, String email) {
        Person person = new Person(document, "Nathaniel", "Silva", email);
        ReflectionTestUtils.setField(person, "id", id);
        return person;
    }

    @Nested
    class Create {

        private final PersonRequest request =
                new PersonRequest("12345678900", "  Nathaniel ", "Silva ", " Nathaniel@Example.COM ");

        @Test
        void shouldPersistNormalizedPersonAndReturnDto() {
            when(personRepository.existsByDocument("12345678900")).thenReturn(false);
            when(personRepository.existsByEmail("nathaniel@example.com")).thenReturn(false);
            when(personRepository.saveAndFlush(any(Person.class))).thenAnswer(invocation -> {
                Person saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            PersonResponse response = personService.create(request);

            ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).saveAndFlush(captor.capture());
            Person saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Nathaniel");
            assertThat(saved.getSurname()).isEqualTo("Silva");
            assertThat(saved.getEmail()).isEqualTo("nathaniel@example.com");

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.document()).isEqualTo("12345678900");
            assertThat(response.email()).isEqualTo("nathaniel@example.com");
        }

        @Test
        void shouldRejectDuplicateDocument() {
            when(personRepository.existsByDocument("12345678900")).thenReturn(true);

            assertThatThrownBy(() -> personService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("documento");
            verify(personRepository, never()).saveAndFlush(any());
        }

        @Test
        void shouldRejectDuplicateEmailIgnoringCase() {
            when(personRepository.existsByDocument("12345678900")).thenReturn(false);
            when(personRepository.existsByEmail("nathaniel@example.com")).thenReturn(true);

            assertThatThrownBy(() -> personService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("e-mail");
            verify(personRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    class Find {

        @Test
        void shouldReturnPersonById() {
            when(personRepository.findById(1L))
                    .thenReturn(Optional.of(person(1L, "12345678900", "nathaniel@example.com")));

            PersonResponse response = personService.findById(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Nathaniel");
        }

        @Test
        void shouldThrowNotFoundWhenPersonDoesNotExist() {
            when(personRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> personService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        void shouldListAllPeopleOrderedById() {
            when(personRepository.findAll(Sort.by("id"))).thenReturn(List.of(
                    person(1L, "12345678900", "a@example.com"),
                    person(2L, "12345678901", "b@example.com")));

            List<PersonResponse> people = personService.findAll();

            assertThat(people).extracting(PersonResponse::id).containsExactly(1L, 2L);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteExistingPerson() {
            Person existing = person(1L, "12345678900", "nathaniel@example.com");
            when(personRepository.findById(1L)).thenReturn(Optional.of(existing));

            personService.delete(1L);

            verify(personRepository).delete(existing);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingMissingPerson() {
            when(personRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> personService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
            verify(personRepository, never()).delete(any());
        }
    }
}
