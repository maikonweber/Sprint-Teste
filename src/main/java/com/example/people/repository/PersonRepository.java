package com.example.people.repository;

import com.example.people.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);
}
