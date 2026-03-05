from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Iterable

from worldtrader.types import AssetClass, Instrument


@dataclass
class InstrumentUniverse:
    numeraire: str
    instruments: Dict[str, Instrument]
    fx_graph: Dict[str, Dict[str, float]]

    @classmethod
    def from_config(cls, cfg: Dict[str, object]) -> "InstrumentUniverse":
        instruments = {}
        for row in cfg["instruments"]:
            inst = Instrument(
                symbol=row["symbol"],
                asset_class=AssetClass(row["asset_class"]),
                venue=row["venue"],
                currency=row["currency"],
                tick_size=row.get("tick_size", 0.01),
                lot_size=row.get("lot_size", 1),
                mid=row["mid"],
                bid=row.get("bid", row["mid"] - 0.01),
                ask=row.get("ask", row["mid"] + 0.01),
                last=row.get("last", row["mid"]),
                volatility_state=row.get("volatility_state", 0.01),
                liquidity_state=row.get("liquidity_state", 1.0),
                fees_bps=row.get("fees_bps", 1.0),
                funding_bps_daily=row.get("funding_bps_daily", 0.0),
                haircut=row.get("haircut", 0.15),
            )
            inst.validate()
            instruments[inst.symbol] = inst
        return cls(numeraire=cfg.get("numeraire", "USD"), instruments=instruments, fx_graph=cfg.get("fx_graph", {}))

    def iter_symbols(self) -> Iterable[str]:
        return self.instruments.keys()
