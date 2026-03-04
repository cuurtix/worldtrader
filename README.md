# WorldTrader

Ce dépôt contient un backend Spring Boot pour l'API de stocks.

## Backend API (port 8000)

### Lancer
```bash
cd backend
mvn spring-boot:run
```

### Endpoints
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{ticker}?view=BASIC`
- `GET /api/v1/stocks/price/{ticker}` (retourne un JSON numérique: `double`)
- `GET /api/v1/stocks/prices?tickers=AAPL,MSFT,TSLA`
- `GET /api/v1/stocks/random`

Swagger/OpenAPI:
- `/swagger-ui/index.html`
- `/v3/api-docs`

### Tests
```bash
cd backend
mvn test
```
