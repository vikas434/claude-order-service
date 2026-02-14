package com.example.orderservice;

import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository paymentRepository;

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Long createOrder(double amount) {
        Order order = new Order();
        order.setCustomerName("Test User");
        order.setItems("Widget");
        order.setTotalAmount(amount);
        return orderRepository.save(order).getId();
    }

    @Test
    @DisplayName("Happy path: create order -> pay -> get payment -> refund -> verify")
    void fullPaymentLifecycle() throws Exception {
        Long orderId = createOrder(500.00);

        Map<String, Object> payReq = Map.of(
                "paymentMethod", "CREDIT_CARD",
                "amount", 500.00,
                "transactionReference", "TXN-001");

        // Pay -> 201
        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.paidAt").exists());

        // Get payment -> 200
        mockMvc.perform(get("/api/orders/{id}/payments", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Refund -> 200
        mockMvc.perform(post("/api/orders/{id}/payments/refund", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        // Order should be CANCELLED
        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        Assertions.assertEquals(Order.OrderStatus.CANCELLED, finalOrder.getStatus());
    }

    @Test
    @DisplayName("Pay for non-existent order -> 404")
    void payNonExistentOrder() throws Exception {
        Map<String, Object> payReq = Map.of("paymentMethod", "UPI", "amount", 100.00);
        mockMvc.perform(post("/api/orders/9999/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Pay wrong amount -> 400 AMOUNT_MISMATCH")
    void payWrongAmount() throws Exception {
        Long orderId = createOrder(500.00);
        Map<String, Object> payReq = Map.of("paymentMethod", "UPI", "amount", 100.00);
        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("AMOUNT_MISMATCH"));
    }

    @Test
    @DisplayName("Pay already-paid order -> 409 PAYMENT_EXISTS")
    void payAlreadyPaidOrder() throws Exception {
        Long orderId = createOrder(500.00);
        Map<String, Object> payReq = Map.of("paymentMethod", "NET_BANKING", "amount", 500.00);

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAYMENT_EXISTS"));
    }

    @Test
    @DisplayName("Refund unpaid order -> 404")
    void refundUnpaidOrder() throws Exception {
        Long orderId = createOrder(500.00);
        mockMvc.perform(post("/api/orders/{id}/payments/refund", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Pay order not in PLACED status -> 400 ORDER_NOT_PLACEABLE")
    void payOrderNotInPlacedStatus() throws Exception {
        Long orderId = createOrder(500.00);
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);

        Map<String, Object> payReq = Map.of("paymentMethod", "DEBIT_CARD", "amount", 500.00);
        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_PLACEABLE"));
    }

    @Test
    @DisplayName("Double refund -> 400 PAYMENT_NOT_REFUNDABLE")
    void refundAlreadyRefundedPayment() throws Exception {
        Long orderId = createOrder(200.00);
        Map<String, Object> payReq = Map.of("paymentMethod", "CREDIT_CARD", "amount", 200.00);

        mockMvc.perform(post("/api/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/{id}/payments/refund", orderId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/payments/refund", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PAYMENT_NOT_REFUNDABLE"));
    }
}
