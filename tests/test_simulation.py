import unittest
from pathlib import Path

from worldtrader.simulation.config import load_config
from worldtrader.simulation.runner import SimulationRunner


class SimulationExportTests(unittest.TestCase):
    def test_export_run(self):
        cfg = load_config("configs/world_market.yaml")
        runner = SimulationRunner(cfg)
        result = runner.run(ticks=5, seed=11)
        out = Path("runs/test_export")
        runner.export(result, out)
        self.assertTrue((out / "metrics.json").exists())


if __name__ == "__main__":
    unittest.main()