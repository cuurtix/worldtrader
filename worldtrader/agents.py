from __future__ import annotations

import random
from dataclasses import dataclass
from typing import Iterable, List

from .models import Order, Side, Stock


@dataclass
class TraderAgent:
    trader_id: str
    aggressiveness: float = 0.02

    def decide_orders(self, tick: int, stocks: Iterable[Stock], rng: random.Random) -> List[Order]:
        orders: List[Order] = []
        for stock in stocks:
            if rng.random() < 0.5:
                side = Side.BUY
                bias = 1 + rng.uniform(-self.aggressiveness, self.aggressiveness)
            else:
                side = Side.SELL
                bias = 1 + rng.uniform(-self.aggressiveness, self.aggressiveness)

            price = max(0.01, stock.last_price * bias)
            qty = rng.randint(1, 15)
            orders.append(
                Order(
                    trader_id=self.trader_id,
                    symbol=stock.symbol,
                    side=side,
                    quantity=qty,
                    limit_price=round(price, 2),
                    timestamp=tick,
                )
            )
        return orders
