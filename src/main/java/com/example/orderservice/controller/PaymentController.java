package com.example.orderservice.controller;

import com.example.orderservice.exception.*;
import com.example.orderservice.model.Payment;
import com.example.orderservice.model.PaymentRequest;
import com.example.orderservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<?> createPayment(@PathVariable Long orderId,
                                           @Valid @RequestBody PaymentRequest request) {
        try {
            Payment payment = paymentService.createPayment(orderId, request);
            return new ResponseEntity<>(payment, HttpStatus.CREATED);
        } catch (OrderNotFoundException e) {
            return buildError(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", e.getMessage());
        } catch (PaymentConflictException e) {
            return buildError(HttpStatus.CONFLICT, "PAYMENT_EXISTS", e.getMessage());
        } catch (PaymentValidationException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getPayment(@PathVariable Long orderId) {
        try {
            return paymentService.getPaymentByOrderId(orderId)
                    .map(p -> ResponseEntity.ok((Object) p))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (OrderNotFoundException e) {
            return buildError(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", e.getMessage());
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long orderId) {
        try {
            Payment payment = paymentService.refundPayment(orderId);
            return ResponseEntity.ok(payment);
        } catch (OrderNotFoundException | PaymentNotFoundException e) {
            return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (PaymentValidationException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> buildError(HttpStatus status, String error, String message) {
        return new ResponseEntity<>(Map.of("error", error, "message", message), status);
    }
}
