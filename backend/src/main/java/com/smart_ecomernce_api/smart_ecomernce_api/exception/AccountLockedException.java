package com.smart_ecomernce_api.smart_ecomernce_api.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}