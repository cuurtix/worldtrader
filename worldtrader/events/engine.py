from __future__ import annotations

import itertools
import random
from dataclasses import dataclass
from typing import List

from worldtrader.types import Event, MacroState


@dataclass
class EventEngine:
    event_intensity: float = 0.02

    def __post_init__(self) -> None:
        self._counter = itertools.count(1)

    def emit(self, tick: int, macro: MacroState, rng: random.Random) -> List[Event]:
        events: List[Event] = []
        if rng.random() < self.event_intensity:
            surprise = rng.gauss(0, 0.2) + 0.1 * macro.inflation
            events.append(Event(f"E{next(self._counter)}", tick, "macro_release", {"surprise": surprise}))
        if rng.random() < self.event_intensity * 0.5:
            shock = abs(rng.gauss(0, 1.0))
            events.append(Event(f"E{next(self._counter)}", tick, "geopolitical", {"shock": shock}))
        return events
