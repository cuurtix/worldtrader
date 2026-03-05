import unittest

from worldtrader.simulation.config import load_config
from worldtrader.simulation.runner import SimulationRunner


class EngineFlowTests(unittest.TestCase):
    def test_runner_generates_outputs(self):
        cfg = load_config("configs/world_market.yaml")
        runner = SimulationRunner(cfg)
        result = runner.run(ticks=20, seed=7)
        self.assertIn("trades", result)
        self.assertIn("snapshots", result)
        self.assertIn("summary", result)
        self.assertGreater(len(result["snapshots"]), 0)


if __name__ == "__main__":
    unittest.main()