package com.example.orderservice.exception;

public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(Long orderId) {
        super("A payment already exists for order id: " + orderId);
    }
}
