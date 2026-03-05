from __future__ import annotations

import random
from dataclasses import dataclass, field
from typing import Dict, List

from worldtrader.types import Order, OrderType, Portfolio, RiskLimits, Side, TimeInForce


@dataclass
class Agent:
    agent_id: str
    latency_ms: int
    risk_limits: RiskLimits
    portfolio: Portfolio
    style: str

    def decide(self, tick: int, ctx: Dict[str, object], rng: random.Random) -> List[Order]:
        return []


@dataclass
class MarketMakerAgent(Agent):
    inventory_aversion: float = 0.1

    def decide(self, tick: int, ctx: Dict[str, object], rng: random.Random) -> List[Order]:
        out: List[Order] = []
        for symbol, inst in ctx["instruments"].items():
            inv = self.portfolio.positions.get(symbol, None)
            inv_qty = inv.qty if inv else 0
            reservation = inst.mid - self.inventory_aversion * inv_qty * inst.tick_size
            spread = inst.mid * max(0.0005, inst.volatility_state * 0.6)
            bid = round(max(inst.tick_size, reservation - spread), 4)
            ask = round(reservation + spread, 4)
            out.append(Order(f"{self.agent_id}-{tick}-{symbol}-b", self.agent_id, symbol, inst.venue, Side.BUY, 10, OrderType.LIMIT, TimeInForce.GTC, bid, timestamp=tick, latency_ms=self.latency_ms))
            out.append(Order(f"{self.agent_id}-{tick}-{symbol}-a", self.agent_id, symbol, inst.venue, Side.SELL, 10, OrderType.LIMIT, TimeInForce.GTC, ask, timestamp=tick, latency_ms=self.latency_ms))
        return out


@dataclass
class CTAAgent(Agent):
    def decide(self, tick: int, ctx: Dict[str, object], rng: random.Random) -> List[Order]:
        out: List[Order] = []
        for symbol, inst in ctx["instruments"].items():
            momentum = ctx["returns"].get(symbol, 0.0)
            if abs(momentum) < inst.volatility_state * 0.2:
                continue
            side = Side.BUY if momentum > 0 else Side.SELL
            qty = max(1, int(30 * min(abs(momentum) / max(inst.volatility_state, 1e-6), 3)))
            out.append(Order(f"{self.agent_id}-{tick}-{symbol}", self.agent_id, symbol, inst.venue, side, qty, OrderType.MARKET, TimeInForce.IOC, timestamp=tick, latency_ms=self.latency_ms))
        return out


@dataclass
class RetailAgent(Agent):
    def decide(self, tick: int, ctx: Dict[str, object], rng: random.Random) -> List[Order]:
        out: List[Order] = []
        for symbol, inst in ctx["instruments"].items():
            if rng.random() < 0.12:
                side = Side.BUY if rng.random() < 0.5 else Side.SELL
                qty = rng.randint(1, 5)
                out.append(Order(f"{self.agent_id}-{tick}-{symbol}", self.agent_id, symbol, inst.venue, side, qty, OrderType.MARKET, TimeInForce.IOC, timestamp=tick, latency_ms=self.latency_ms))
        return out


def build_agents(cfg: Dict[str, object], instruments: Dict[str, object]) -> List[Agent]:
    agents: List[Agent] = []
    for row in cfg["agents"]:
        base = dict(
            agent_id=row["id"],
            latency_ms=row.get("latency_ms", 5),
            risk_limits=RiskLimits(row.get("max_leverage", 5.0), row.get("max_drawdown", 0.4), row.get("max_concentration", 0.35)),
            portfolio=Portfolio(agent_id=row["id"], base_ccy=cfg.get("numeraire", "USD")),
            style=row["type"],
        )
        if row["type"] == "market_maker":
            agents.append(MarketMakerAgent(**base))
        elif row["type"] == "cta":
            agents.append(CTAAgent(**base))
        else:
            agents.append(RetailAgent(**base))

    for ag in agents:
        ag.portfolio.margin.cash = cfg.get("initial_cash", 5_000_000.0)
        ag.portfolio.margin.equity = ag.portfolio.margin.cash
    return agents
