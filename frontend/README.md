# WorldTrader Retail Frontend

## Run
```bash
cp .env.example .env
npm install
npm run dev
```

## Env
- `VITE_API_BASE=http://localhost:8000`
- `VITE_OFFLINE_DEMO=true` (optional, intercepts API calls with local mock data)

## Data flow
- Chart history: `GET /api/v1/candles?ticker=AAPL&tf=1s|5s|1m&limit=500`
  - Response: `[{ t, o, h, l, c, v }]` with `t` in epoch seconds.
- Trades tape: `GET /api/v1/trades?ticker=AAPL&limit=200`
  - Response: `[{ t, price, qty, side }]`.
- Order book: `GET /api/v1/orderbook/{ticker}`
- Account: `GET /api/v1/portfolio/{traderId}`

The UI polls market data every ~750ms and writes debug logs in browser console:
- API base URL
- current ticker
- payload count + sample for candles/trades/orderbook/account
- fetch errors with status/body

Debug page: `http://localhost:5173/debug`

## Tests
```bash
npm run test
npm run test:e2e
```
