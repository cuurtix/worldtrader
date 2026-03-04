# WorldTrader

Simulation boursière originale inspirée des fonctionnalités classiques d'un moteur de marché:

- carnet d'ordres (bid/ask)
- matching prix/temps
- gestion de portefeuille
- agents de trading simples
- boucle de simulation par ticks
- export des résultats

## Lancement

```bash
python -m worldtrader.simulation --ticks 50 --seed 42
```

## Tests

```bash
python -m unittest discover -s tests -v
```
