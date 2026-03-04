package com.smart_ecomernce_api.smart_ecomernce_api.exception;


public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                productName, available, requested));
    }

}
