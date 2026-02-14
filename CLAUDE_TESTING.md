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
