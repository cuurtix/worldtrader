from __future__ import annotations

import argparse
import random
from dataclasses import asdict
from typing import Dict, List

from .agents import TraderAgent
from .engine import MarketEngine
from .models import Portfolio, Stock


def run_simulation(ticks: int, seed: int) -> Dict[str, object]:
    rng = random.Random(seed)

    stocks = [
        Stock("ACME", 100.0),
        Stock("BNR", 55.0),
        Stock("CST", 210.0),
    ]
    engine = MarketEngine(stocks, fee_rate=0.001)

    agents = [TraderAgent(f"T{i}", aggressiveness=0.03) for i in range(1, 7)]

    for agent in agents:
        engine.register_trader(
            agent.trader_id,
            Portfolio(cash=100_000.0, positions={s.symbol: 200 for s in stocks}),
        )

    for tick in range(1, ticks + 1):
        shuffled = agents[:]
        rng.shuffle(shuffled)
        for agent in shuffled:
            for order in agent.decide_orders(tick, stocks, rng):
                engine.submit_order(order, tick)

    leaderboard = sorted(
        (
            {
                "trader": agent.trader_id,
                "value": round(engine.mark_to_market_value(agent.trader_id), 2),
            }
            for agent in agents
        ),
        key=lambda x: x["value"],
        reverse=True,
    )

    return {
        "ticks": ticks,
        "trades": [asdict(t) for t in engine.trades],
        "stocks": {sym: st.last_price for sym, st in engine.stocks.items()},
        "leaderboard": leaderboard,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the WorldTrader simulation")
    parser.add_argument("--ticks", type=int, default=50)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    result = run_simulation(ticks=args.ticks, seed=args.seed)
    print(f"Ticks: {result['ticks']}")
    print(f"Trades executed: {len(result['trades'])}")
    print("Final stock prices:")
    for symbol, price in result["stocks"].items():
        print(f"  - {symbol}: {price:.2f}")
    print("Top 3 traders:")
    for row in result["leaderboard"][:3]:
        print(f"  - {row['trader']}: {row['value']:.2f}")


if __name__ == "__main__":
    main()
