package com.example.people.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException person(Long id) {
        return new ResourceNotFoundException("Pessoa com id " + id + " não encontrada");
    }
}
