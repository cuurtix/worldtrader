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
- `GET /candles?asset=AAPL&tf=1&limit=200`
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


### Format JSON WebSocket (`/ws/ticks`)
Chaque trade diffusé en temps réel envoie:
```json
{
  "symbol": "AAPL",
  "ts": 1712345678123,
  "price": 190.25,
  "size": 12,
  "capitalisation": 2983000000000
}
```
(Compatibilité conservée avec les champs `asset`, `time`, `volume`.)

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


## Cohérence prix/capitalisation
Le moteur maintient strictement la relation **capitalisation = prix × actions en circulation** pour chaque actif.
- Chaque trade met à jour instantanément le prix puis la capitalisation.
- Les splits ajustent simultanément prix et nombre d'actions en circulation.
- Les dividendes ajustent le prix ex-dividende et recalculent la capitalisation.
