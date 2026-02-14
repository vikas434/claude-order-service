package com.example.orderservice.service;

import com.example.orderservice.exception.*;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.Payment;
import com.example.orderservice.model.PaymentRequest;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Payment createPayment(Long orderId, PaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != Order.OrderStatus.PLACED) {
            throw new PaymentValidationException("ORDER_NOT_PLACEABLE",
                    "Order " + orderId + " is not in PLACED status. Current status: " + order.getStatus());
        }

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new PaymentConflictException(orderId);
        }

        if (!amountsMatch(request.getAmount(), order.getTotalAmount())) {
            throw new PaymentValidationException("AMOUNT_MISMATCH",
                    "Payment amount " + request.getAmount() + " does not match order total " + order.getTotalAmount());
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionReference(request.getTransactionReference());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        return paymentRepository.save(payment);
    }

    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public Payment refundPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new PaymentValidationException("PAYMENT_NOT_REFUNDABLE",
                    "Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        order.setStatus(Order.OrderStatus.CANCELLED);

        orderRepository.save(order);
        return paymentRepository.save(payment);
    }

    private boolean amountsMatch(Double requestAmount, Double orderAmount) {
        if (requestAmount == null || orderAmount == null) return false;
        return Math.abs(requestAmount - orderAmount) < 0.001;
    }
}
