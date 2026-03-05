# WorldTrader

WorldTrader fournit maintenant deux couches complémentaires :

1. **Backend Java microstructure** (API `api/v1/*` existante, conservée).
2. **Simulateur temps réel Python jouable** lancé avec `python run.py`.

## Lancement rapide du simulateur temps réel
```bash
python run.py
```
Puis ouvrir : `http://localhost:8000`.

L'interface est entièrement en français :
- Acheter / Vendre
- Carnet d'ordres
- Ticket d'ordre
- Portefeuille joueur
- Historique
- Ordres en attente

## API temps réel (FastAPI)
- `GET /marche`
- `GET /orderbook?asset=AAPL`
- `POST /ordre`
- `POST /annuler`
- `GET /portefeuille`
- `WS /ws/ticks`

### Exemple ordre
```bash
curl -X POST http://localhost:8000/ordre \
  -H "Content-Type: application/json" \
  -d '{"asset":"AAPL","side":"BUY","taille":10,"type_ordre":"LIMIT","prix_limite":189.9}'
```

## Caractéristiques de simulation implémentées
- CLOB multi-actifs (`AAPL`, `BTC`, `GOLD`, `OIL`).
- Matching price-time, partial fills, market/limit, cancellations.
- Boucle **event-driven** temps réel (20 cycles/s).
- Flux ordres Poisson + tailles Pareto (heavy-tail).
- Régimes de volatilité (faible/moyenne/haute) + clustering.
- Sentiment (`fear`, `greed`, `panic`, `confidence`).
- Latence aléatoire d'exécution (1ms–50ms).
- Scénarios flash-crash (retrait liquidité + ventes institutionnelles).
- Joueur tradable (ordres, annulation, portefeuille/PnL/historique).
- Classement + succès (`premier_trade`, `profit_1000`).
- Logging CSV :
  - `runs/realtime_logs/trades.csv`
  - `runs/realtime_logs/prices.csv`
  - `runs/realtime_logs/orderbook.csv`
  - `runs/realtime_logs/agent_activity.csv`

## Tests
```bash
python -m unittest discover -s tests -q
```
