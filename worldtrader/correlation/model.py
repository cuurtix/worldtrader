from __future__ import annotations

from dataclasses import dataclass
from typing import Dict


@dataclass
class CorrelationModel:
    base_corr: float = 0.2

    def matrix(self, symbols: list[str], stress: float) -> Dict[str, Dict[str, float]]:
        out: Dict[str, Dict[str, float]] = {}
        for a in symbols:
            out[a] = {}
            for b in symbols:
                if a == b:
                    out[a][b] = 1.0
                else:
                    out[a][b] = min(0.95, self.base_corr + 0.5 * stress)
        return out
