from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ImpactModel:
    alpha: float = 0.6
    beta: float = 0.4
    temp_scale: float = 0.01
    perm_scale: float = 0.003

    def temporary_impact(self, signed_qty: float, liquidity: float) -> float:
        return self.temp_scale * (abs(signed_qty) / max(liquidity, 1e-9)) ** self.alpha * (1 if signed_qty >= 0 else -1)

    def permanent_impact(self, signed_qty: float, liquidity: float) -> float:
        return self.perm_scale * (abs(signed_qty) / max(liquidity, 1e-9)) ** self.beta * (1 if signed_qty >= 0 else -1)


@dataclass
class SpreadModel:
    base_spread_bps: float = 5.0

    def compute(self, vol_state: float, imbalance: float, liquidity_state: float, stress: float) -> float:
        spread_bps = self.base_spread_bps * (1 + 4 * vol_state + abs(imbalance) + stress) / max(liquidity_state, 0.2)
        return spread_bps / 10_000
