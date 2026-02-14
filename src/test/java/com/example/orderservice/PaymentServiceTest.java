package com.example.orderservice;

import com.example.orderservice.exception.*;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.Payment;
import com.example.orderservice.model.PaymentRequest;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.PaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @InjectMocks PaymentService paymentService;

    private Order placedOrder(Long id, double amount) {
        Order o = new Order();
        o.setId(id);
        o.setTotalAmount(amount);
        o.setStatus(Order.OrderStatus.PLACED);
        return o;
    }

    @Test
    @DisplayName("createPayment succeeds for valid input")
    void createPayment_success() {
        Order order = placedOrder(1L, 100.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentRequest req = new PaymentRequest();
        req.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        req.setAmount(100.0);

        Payment result = paymentService.createPayment(1L, req);
        assertEquals(Payment.PaymentStatus.SUCCESS, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("createPayment throws OrderNotFoundException when order missing")
    void createPayment_orderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        PaymentRequest req = new PaymentRequest();
        req.setAmount(100.0);
        req.setPaymentMethod(Payment.PaymentMethod.UPI);
        assertThrows(OrderNotFoundException.class, () -> paymentService.createPayment(99L, req));
    }

    @Test
    @DisplayName("createPayment throws PaymentValidationException for amount mismatch")
    void createPayment_amountMismatch() {
        Order order = placedOrder(1L, 500.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        PaymentRequest req = new PaymentRequest();
        req.setAmount(100.0);
        req.setPaymentMethod(Payment.PaymentMethod.UPI);

        PaymentValidationException ex = assertThrows(PaymentValidationException.class,
                () -> paymentService.createPayment(1L, req));
        assertEquals("AMOUNT_MISMATCH", ex.getErrorCode());
    }

    @Test
    @DisplayName("createPayment throws PaymentConflictException when payment exists")
    void createPayment_duplicate() {
        Order order = placedOrder(1L, 100.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        PaymentRequest req = new PaymentRequest();
        req.setAmount(100.0);
        req.setPaymentMethod(Payment.PaymentMethod.DEBIT_CARD);

        assertThrows(PaymentConflictException.class, () -> paymentService.createPayment(1L, req));
    }

    @Test
    @DisplayName("refundPayment succeeds when payment is SUCCESS")
    void refundPayment_success() {
        Order order = placedOrder(1L, 100.0);
        Payment payment = new Payment();
        payment.setOrderId(1L);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.refundPayment(1L);
        assertEquals(Payment.PaymentStatus.REFUNDED, result.getStatus());
        assertEquals(Order.OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("refundPayment throws when payment not SUCCESS")
    void refundPayment_notRefundable() {
        Order order = placedOrder(1L, 100.0);
        Payment payment = new Payment();
        payment.setStatus(Payment.PaymentStatus.REFUNDED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentValidationException ex = assertThrows(PaymentValidationException.class,
                () -> paymentService.refundPayment(1L));
        assertEquals("PAYMENT_NOT_REFUNDABLE", ex.getErrorCode());
    }
}
