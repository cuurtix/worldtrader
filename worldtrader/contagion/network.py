from __future__ import annotations

from dataclasses import dataclass
from typing import Dict


@dataclass
class ContagionNetwork:
    exposures: Dict[str, Dict[str, float]]

    def propagate(self, losses: Dict[str, float]) -> Dict[str, float]:
        propagated = dict(losses)
        for src, links in self.exposures.items():
            for dst, w in links.items():
                propagated[dst] = propagated.get(dst, 0.0) + losses.get(src, 0.0) * w
        return propagated
