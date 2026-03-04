from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict


class Side(str, Enum):
    BUY = "BUY"
    SELL = "SELL"


@dataclass(slots=True)
class Stock:
    symbol: str
    last_price: float


@dataclass(slots=True)
class Order:
    trader_id: str
    symbol: str
    side: Side
    quantity: int
    limit_price: float
    timestamp: int


@dataclass
class Portfolio:
    cash: float
    positions: Dict[str, int] = field(default_factory=dict)

    def can_buy(self, total_cost: float) -> bool:
        return self.cash >= total_cost

    def can_sell(self, symbol: str, quantity: int) -> bool:
        return self.positions.get(symbol, 0) >= quantity

    def apply_buy(self, symbol: str, quantity: int, total_cost: float) -> None:
        self.cash -= total_cost
        self.positions[symbol] = self.positions.get(symbol, 0) + quantity

    def apply_sell(self, symbol: str, quantity: int, total_proceeds: float) -> None:
        self.cash += total_proceeds
        self.positions[symbol] = self.positions.get(symbol, 0) - quantity
