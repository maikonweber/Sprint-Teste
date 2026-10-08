package com.example.people.exception;

/**
 * Violação de regra de negócio. Subclasses mais específicas podem ser mapeadas
 * para códigos HTTP próprios no {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
