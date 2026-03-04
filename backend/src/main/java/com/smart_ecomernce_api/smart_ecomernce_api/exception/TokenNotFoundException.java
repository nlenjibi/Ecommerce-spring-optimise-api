package com.smart_ecomernce_api.smart_ecomernce_api.exception;

/**
 * Exception thrown when token is not found
 */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
