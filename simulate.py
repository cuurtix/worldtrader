from __future__ import annotations

import argparse
from datetime import datetime
from pathlib import Path

from worldtrader.simulation.config import load_config
from worldtrader.simulation.runner import SimulationRunner


def main() -> None:
    parser = argparse.ArgumentParser(description="WorldTrader multi-asset simulator")
    parser.add_argument("--config", required=True)
    parser.add_argument("--ticks", type=int, default=200)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--log-level", default="INFO")
    args = parser.parse_args()

    cfg = load_config(args.config)
    runner = SimulationRunner(cfg)
    result = runner.run(ticks=args.ticks, seed=args.seed)
    stamp = datetime.utcnow().strftime("%Y%m%d_%H%M%S")
    out_dir = Path("runs") / stamp
    runner.export(result, out_dir)
    print(f"Run complete. trades={len(result['trades'])} output={out_dir}")
    print("Stylized facts summary:")
    for symbol, row in result["summary"].items():
        print(symbol, row)


if __name__ == "__main__":
    main()
