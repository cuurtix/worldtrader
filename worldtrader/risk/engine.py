from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List

from typing import Any

from worldtrader.types import Instrument, Order, OrderType, Side, TimeInForce


@dataclass
class RiskManager:
    maintenance_margin: float = 0.25

    def update_equity(self, agent: Any, instruments: Dict[str, Instrument]) -> None:
        mtm = agent.portfolio.margin.cash
        exposure = 0.0
        for symbol, pos in agent.portfolio.positions.items():
            px = instruments[symbol].mid
            mtm += pos.qty * px
            exposure += abs(pos.qty * px)
        agent.portfolio.margin.equity = mtm
        agent.portfolio.margin.used_margin = exposure * 0.1

    def liquidation_orders(self, agent: Any, instruments: Dict[str, Instrument], tick: int) -> List[Order]:
        margin = agent.portfolio.margin
        if margin.equity >= max(1.0, margin.used_margin * self.maintenance_margin):
            return []

        orders: List[Order] = []
        positions = sorted(agent.portfolio.positions.values(), key=lambda p: abs(p.qty), reverse=True)
        for pos in positions:
            if pos.qty == 0:
                continue
            inst = instruments[pos.symbol]
            side = Side.SELL if pos.qty > 0 else Side.BUY
            qty = max(1, abs(pos.qty) // 2)
            orders.append(Order(f"liq-{agent.agent_id}-{tick}-{pos.symbol}", agent.agent_id, pos.symbol, inst.venue, side, qty, OrderType.MARKET, TimeInForce.IOC, timestamp=tick, latency_ms=0))
        return orders
