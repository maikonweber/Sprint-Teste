package com.example.people.exception;

/**
 * Falha ao se comunicar com um serviço externo (indisponível, timeout, resposta inválida).
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
