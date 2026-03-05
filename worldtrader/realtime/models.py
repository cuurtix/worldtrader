from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class Side(str, Enum):
    BUY = "BUY"
    SELL = "SELL"


class OrderType(str, Enum):
    LIMIT = "LIMIT"
    MARKET = "MARKET"


@dataclass
class Order:
    order_id: str
    agent_id: str
    symbol: str
    side: Side
    size: int
    timestamp: float
    order_type: OrderType = OrderType.LIMIT
    price: Optional[float] = None


@dataclass
class Trade:
    trade_id: str
    symbol: str
    price: float
    size: int
    buy_agent: str
    sell_agent: str
    timestamp: float


@dataclass
class Portfolio:
    cash: float = 100_000.0
    positions: dict[str, int] = field(default_factory=dict)
    avg_cost: dict[str, float] = field(default_factory=dict)
    history: list[dict] = field(default_factory=list)


@dataclass
class MarketRegime:
    name: str = "moyenne_volatilite"
    sigma: float = 0.004
    burst_prob: float = 0.02


@dataclass
class Sentiment:
    fear: float = 0.2
    greed: float = 0.5
    panic: float = 0.1
    confidence: float = 0.6
