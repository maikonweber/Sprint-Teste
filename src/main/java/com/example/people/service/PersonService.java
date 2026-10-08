package com.example.people.service;

import com.example.people.dto.person.PersonRequest;
import com.example.people.dto.person.PersonResponse;
import com.example.people.entity.Person;
import com.example.people.exception.DuplicateResourceException;
import com.example.people.exception.ResourceNotFoundException;
import com.example.people.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private static final Logger log = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Transactional
    public PersonResponse create(PersonRequest request) {
        String document = request.document().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (personRepository.existsByDocument(document)) {
            throw new DuplicateResourceException("Já existe uma pessoa cadastrada com este documento");
        }
        if (personRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Já existe uma pessoa cadastrada com este e-mail");
        }

        Person person = new Person(document, request.name().trim(), request.surname().trim(), email);
        Person saved = personRepository.saveAndFlush(person);
        log.info("Pessoa cadastrada: id={}", saved.getId());
        return PersonResponse.from(saved);
    }

    public List<PersonResponse> findAll() {
        return personRepository.findAll(Sort.by("id")).stream()
                .map(PersonResponse::from)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return personRepository.findById(id)
                .map(PersonResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.person(id));
    }

    @Transactional
    public void delete(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.person(id));
        personRepository.delete(person);
        log.info("Pessoa excluída: id={}", id);
    }
}
