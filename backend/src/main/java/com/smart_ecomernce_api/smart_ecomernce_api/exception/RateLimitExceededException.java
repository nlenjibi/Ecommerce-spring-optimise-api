package com.smart_ecomernce_api.smart_ecomernce_api.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}