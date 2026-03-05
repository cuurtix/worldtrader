from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List


@dataclass
class ReplayStore:
    snapshots: List[Dict[str, object]] = field(default_factory=list)

    def save_snapshot(self, tick: int, state: Dict[str, object]) -> None:
        self.snapshots.append({"tick": tick, **state})
