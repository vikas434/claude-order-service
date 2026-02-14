# Claude Testing - Order Service

This project was built and tested entirely using Claude Code (multi-agent workflow).

## Test Results

All CRUD endpoints verified via `curl` against the running application:

| # | Operation | Endpoint              | Expected | Actual | Status |
|---|----------|-----------------------|----------|--------|--------|
| 1 | Create   | POST /api/orders      | 201      | 201    | PASS   |
| 2 | Get by ID| GET /api/orders/1     | 200      | 200    | PASS   |
| 3 | Update   | PUT /api/orders/1     | 200      | 200    | PASS   |
| 4 | List All | GET /api/orders       | 200      | 200    | PASS   |
| 5 | Delete   | DELETE /api/orders/1  | 204      | 204    | PASS   |
| 6 | Get deleted | GET /api/orders/1  | 404      | 404    | PASS   |

## Sample Order Used

```json
{
  "customerName": "Jane Doe",
  "items": "Laptop, Mouse, Keyboard",
  "totalAmount": 1549.99
}
```

Auto-assigned fields: `id=1`, `status=PLACED`, `createdAt` set to current timestamp.

## How to Run

```bash
cd order-service
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:orderdb`, user: `sa`, no password)

---

## Payment API (Spec-Driven Development)

OpenAPI spec: [`specs/payment-api.yaml`](specs/payment-api.yaml) — written first as the authoritative contract.

### Automated Test Results — 13/13 PASS

**Integration Tests (PaymentIntegrationTest) — 7/7**

| # | Scenario | Endpoint | Expected | Spec Rule |
|---|----------|----------|----------|-----------|
| 1 | Full lifecycle: pay + get + refund | POST/GET/POST | 201/200/200 | Happy path |
| 2 | Non-existent order | POST /api/orders/9999/payments | 404 | Order must exist |
| 3 | Wrong amount | POST /api/orders/{id}/payments | 400 AMOUNT_MISMATCH | Amount must match |
| 4 | Already paid order | POST /api/orders/{id}/payments | 409 PAYMENT_EXISTS | One payment per order |
| 5 | Refund unpaid order | POST /api/orders/{id}/payments/refund | 404 | Payment must exist |
| 6 | Order not PLACED | POST /api/orders/{id}/payments | 400 ORDER_NOT_PLACEABLE | Order must be PLACED |
| 7 | Double refund | POST /api/orders/{id}/payments/refund | 400 PAYMENT_NOT_REFUNDABLE | Only SUCCESS refundable |

**Unit Tests (PaymentServiceTest) — 6/6**

| # | Scenario | Assertion |
|---|----------|-----------|
| 1 | Valid payment | Status = SUCCESS |
| 2 | Order not found | Throws OrderNotFoundException |
| 3 | Amount mismatch | Throws PaymentValidationException (AMOUNT_MISMATCH) |
| 4 | Duplicate payment | Throws PaymentConflictException |
| 5 | Valid refund | Status = REFUNDED, order = CANCELLED |
| 6 | Non-refundable | Throws PaymentValidationException (PAYMENT_NOT_REFUNDABLE) |
