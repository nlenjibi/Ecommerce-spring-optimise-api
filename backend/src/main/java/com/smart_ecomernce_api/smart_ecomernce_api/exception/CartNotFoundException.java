package com.smart_ecomernce_api.smart_ecomernce_api.exception;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException() {
        super("Cart not found");
    }

    public CartNotFoundException(Long cartId) {
        super("Cart not found with ID: " + cartId);
    }

    public CartNotFoundException(String message) {
        super(message);
    }
}
