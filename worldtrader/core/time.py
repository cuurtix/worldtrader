from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone


@dataclass
class MarketClock:
    current: datetime
    tick_seconds: int = 1
    tick: int = 0

    def advance(self) -> datetime:
        self.tick += 1
        self.current = self.current + timedelta(seconds=self.tick_seconds)
        return self.current

    def intraday_seasonality(self) -> float:
        hour = self.current.hour
        if hour in (8, 9, 15, 16):
            return 1.3
        if hour in (11, 12):
            return 0.8
        return 1.0


def default_clock() -> MarketClock:
    return MarketClock(current=datetime(2025, 1, 2, 8, 0, 0, tzinfo=timezone.utc), tick_seconds=60)
