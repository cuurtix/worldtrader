from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Dict, List, Optional


class AssetClass(str, Enum):
    EQUITY = "equity"
    FX = "fx"
    CRYPTO = "crypto"
    COMMODITY = "commodity"
    RATE = "rate"
    INDEX = "index"
    DERIVATIVE = "derivative"


class Side(str, Enum):
    BUY = "buy"
    SELL = "sell"


class OrderType(str, Enum):
    MARKET = "market"
    LIMIT = "limit"
    STOP = "stop"
    STOP_LIMIT = "stop_limit"


class TimeInForce(str, Enum):
    GTC = "gtc"
    IOC = "ioc"
    FOK = "fok"


@dataclass
class Instrument:
    symbol: str
    asset_class: AssetClass
    venue: str
    currency: str
    tick_size: float
    lot_size: int
    mid: float
    bid: float
    ask: float
    last: float
    volatility_state: float = 0.01
    liquidity_state: float = 1.0
    fees_bps: float = 1.0
    funding_bps_daily: float = 0.0
    haircut: float = 0.15
    corporate_actions: List[Dict[str, float]] = field(default_factory=list)

    def validate(self) -> None:
        assert self.tick_size > 0
        assert self.lot_size > 0
        assert self.bid <= self.ask

    def to_dict(self) -> Dict[str, object]:
        return asdict(self)


@dataclass
class Venue:
    name: str
    timezone: str
    maker_fee_bps: float
    taker_fee_bps: float
    latency_ms: int
    session_type: str = "cash"

    def to_dict(self) -> Dict[str, object]:
        return asdict(self)


@dataclass
class Order:
    order_id: str
    agent_id: str
    instrument: str
    venue: str
    side: Side
    qty: int
    order_type: OrderType
    tif: TimeInForce = TimeInForce.GTC
    limit_price: Optional[float] = None
    stop_price: Optional[float] = None
    post_only: bool = False
    iceberg_peak: Optional[int] = None
    timestamp: int = 0
    latency_ms: int = 0

    def validate(self) -> None:
        assert self.qty > 0
        if self.order_type in (OrderType.LIMIT, OrderType.STOP_LIMIT):
            assert self.limit_price is not None and self.limit_price > 0

    def to_dict(self) -> Dict[str, object]:
        out = asdict(self)
        out["side"] = self.side.value
        out["order_type"] = self.order_type.value
        out["tif"] = self.tif.value
        return out


@dataclass
class Trade:
    trade_id: str
    instrument: str
    venue: str
    price: float
    qty: int
    buyer: str
    seller: str
    tick: int

    def to_dict(self) -> Dict[str, object]:
        return asdict(self)


@dataclass
class Position:
    symbol: str
    qty: int = 0
    avg_price: float = 0.0


@dataclass
class MarginAccount:
    cash: float
    equity: float
    used_margin: float = 0.0
    maintenance_ratio: float = 0.25


@dataclass
class Portfolio:
    agent_id: str
    base_ccy: str
    positions: Dict[str, Position] = field(default_factory=dict)
    margin: MarginAccount = field(default_factory=lambda: MarginAccount(0.0, 0.0))
    realized_pnl: float = 0.0


@dataclass
class MacroState:
    growth: float
    inflation: float
    policy_rate: float
    liquidity: float
    risk_appetite: float


@dataclass
class RegimeState:
    name: str
    stress: float
    corr_multiplier: float


@dataclass
class Event:
    event_id: str
    tick: int
    kind: str
    payload: Dict[str, float]

    def to_dict(self) -> Dict[str, object]:
        return asdict(self)


@dataclass
class RiskLimits:
    max_leverage: float
    max_drawdown: float
    max_concentration: float
