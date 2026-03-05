from __future__ import annotations

import random
from dataclasses import dataclass

from worldtrader.types import MacroState


@dataclass
class MacroEngine:
    state: MacroState

    def update(self, rng: random.Random) -> MacroState:
        self.state.growth += rng.gauss(0, 0.01)
        self.state.inflation += rng.gauss(0, 0.01)
        self.state.liquidity = max(0.2, self.state.liquidity + rng.gauss(0, 0.02))
        self.state.risk_appetite = min(2.0, max(-2.0, self.state.risk_appetite + rng.gauss(0, 0.05)))
        self.state.policy_rate = max(0.0, self.state.policy_rate + 0.02 * (self.state.inflation - 0.02) - 0.01 * self.state.growth)
        return self.state
