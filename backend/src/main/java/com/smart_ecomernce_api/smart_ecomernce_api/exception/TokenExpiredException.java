package com.smart_ecomernce_api.smart_ecomernce_api.exception;

/**
 * Exception thrown when token has expired
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
