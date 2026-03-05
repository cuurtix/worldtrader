from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List

from worldtrader.types import Order, Trade, Venue


@dataclass
class InflightOrder:
    release_tick: int
    order: Order


@dataclass
class VenueEngine:
    venues: Dict[str, Venue]
    inflight: Dict[str, List[InflightOrder]] = field(default_factory=dict)

    def schedule(self, order: Order, current_tick: int) -> None:
        latency_ticks = max(1, (order.latency_ms + self.venues[order.venue].latency_ms) // 50)
        release_tick = current_tick + latency_ticks
        self.inflight.setdefault(order.venue, []).append(InflightOrder(release_tick, order))

    def release(self, tick: int) -> List[Order]:
        ready: List[Order] = []
        for venue, queue in self.inflight.items():
            remaining = []
            for pending in queue:
                if pending.release_tick <= tick:
                    ready.append(pending.order)
                else:
                    remaining.append(pending)
            self.inflight[venue] = remaining
        return ready

    def fee_bps(self, venue: str, is_taker: bool) -> float:
        v = self.venues[venue]
        return v.taker_fee_bps if is_taker else v.maker_fee_bps
