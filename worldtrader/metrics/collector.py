from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List


@dataclass
class MetricsCollector:
    returns: Dict[str, List[float]] = field(default_factory=dict)
    events_log: List[dict] = field(default_factory=list)
    stress_index: List[float] = field(default_factory=list)

    def collect_return(self, symbol: str, r: float) -> None:
        self.returns.setdefault(symbol, []).append(r)

    def collect_event(self, event: dict) -> None:
        self.events_log.append(event)

    def collect_stress(self, stress: float) -> None:
        self.stress_index.append(stress)

    def export(self, out_dir: Path, trades: List[dict], snapshots: List[dict]) -> None:
        out_dir.mkdir(parents=True, exist_ok=True)
        self._write_records(out_dir / "trades.parquet", trades)
        self._write_records(out_dir / "orderbook_snapshots.parquet", snapshots)
        (out_dir / "metrics.json").write_text(json.dumps({"returns": self.returns, "stress_index": self.stress_index, "events": self.events_log}, indent=2), encoding="utf-8")

    @staticmethod
    def _write_records(path: Path, records: List[dict]) -> None:
        try:
            import pandas as pd  # type: ignore
            df = pd.DataFrame(records)
            df.to_parquet(path, index=False)
        except Exception:
            path.write_text("\n".join(json.dumps(r) for r in records), encoding="utf-8")
