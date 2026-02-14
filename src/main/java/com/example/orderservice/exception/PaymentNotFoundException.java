package com.example.orderservice.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long orderId) {
        super("No payment found for order id: " + orderId);
    }
}
