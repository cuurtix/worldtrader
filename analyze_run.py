from __future__ import annotations

import json
import sys
from pathlib import Path

from worldtrader.calibration.stylized import analyze_all


def main() -> None:
    run_dir = Path(sys.argv[1])
    metrics = json.loads((run_dir / "metrics.json").read_text(encoding="utf-8"))
    report = analyze_all(metrics.get("returns", {}))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
