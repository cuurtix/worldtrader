# Secure Order API

## POST /api/v2/orders

Asynchronous order intake endpoint.

### Headers
- `X-User-Id`: authenticated account owner id
- `X-Correlation-Id`: optional correlation id

### Request body
```json
{
  "userId": "trader-001",
  "symbol": "BTC-USD",
  "orderType": "LIMIT",
  "quantity": 2,
  "price": 62000.12345678,
  "fee": 1.00000000
}
```

### Response: 202 Accepted
```json
{
  "orderId": "f98d2f14-2e35-4ad8-bb18-1a502b8df2de",
  "status": "QUEUED"
}
```

### Errors
- `400` InvalidOrderError
- `402` InsufficientFundsError
- `409` MarketClosedError
- `500` SystemError
