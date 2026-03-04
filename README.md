# WorldTrader Backend (Spring Boot)

Backend orienté **microstructure** pour simulation de marché (order book + matching + agents + régime), sans frontend.

## Run
```bash
cd backend
mvn spring-boot:run
```

## Endpoints existants conservés
- `GET /api/v1/stocks`
- `GET /api/v1/stocks/{ticker}?view=BASIC`
- `GET /api/v1/stocks/price/{ticker}`
- `GET /api/v1/market`
- `POST /api/v1/market/pause`
- `POST /api/v1/market/resume`
- `POST /api/v1/market/interval?millis=250`

## Nouveaux endpoints microstructure
- `GET /api/v1/orderbook/{ticker}?levels=20`
- `POST /api/v1/orders`
- `DELETE /api/v1/orders/{orderId}`
- `GET /api/v1/trades/{ticker}?limit=200`
- `GET /api/v1/portfolio/{traderId}`
- `GET /api/v1/regime`
- `PUT /api/v1/regime`
- `GET /api/v1/metrics/{ticker}`

## Exemple order submit
```bash
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"traderId":"PLAYER_1","ticker":"AAPL","side":"BUY","type":"LIMIT","qty":10,"price":189.8,"tif":"GTC"}'
```

## Exemple PowerShell
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8000/api/v1/orders" -ContentType "application/json" -Body '{"traderId":"PLAYER_1","ticker":"AAPL","side":"SELL","type":"MARKET","qty":5,"tif":"IOC"}'
```

## Modèle implémenté
- Carnet L2 par ticker (`TreeMap` bid/ask) + priorité prix/FIFO
- Matching limit/market, partial fills, cancel
- Trades en ring buffer in-memory
- Portefeuilles in-memory (cash/positions/realized/unrealized PnL)
- Agents invisibles: MarketMaker, Noise, Momentum/FOMO, MeanReversion
- Régime (`riskOnOff`, `centralBank`, `politicalStress`, `liquidity`, `newsIntensity`, `volatilityTarget`)
- Métriques: mid/spread/depth, OFI/NOFI, liquidity imbalance, RV, VWAP

## Tests
```bash
cd backend
mvn test
```
