# WorldTrader — Plateforme de simulation multi-actifs mondiale

WorldTrader est un moteur de simulation **modulaire** couvrant:

- univers multi-actifs (equity, FX, crypto, commodity),
- microstructure (LOB, price-time FIFO, latence, impact concave, spread dynamique),
- agents hétérogènes (market maker, CTA, retail),
- macro/régimes/news,
- risk engine (marge + liquidations),
- contagion réseau,
- calibration “stylized facts” + replay déterministe.

## Exécution

```bash
python simulate.py --config configs/world_market.yaml --ticks 200 --seed 42 --log-level INFO
```

Exports dans `runs/<timestamp>/`:

- `trades.parquet`
- `orderbook_snapshots.parquet`
- `metrics.json`

## Analyse stylized facts

```bash
python analyze_run.py runs/<timestamp>
```

## Tests

```bash
python -m unittest discover -s tests -v
```
