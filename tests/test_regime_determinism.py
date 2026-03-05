import unittest

from worldtrader.simulation.config import load_config
from worldtrader.simulation.runner import SimulationRunner


class RegimeDeterminismTests(unittest.TestCase):
    def test_seed_replay_deterministic(self):
        cfg = load_config("configs/world_market.yaml")
        r = SimulationRunner(cfg)
        a = r.run(40, 123)
        b = r.run(40, 123)
        self.assertEqual(len(a["trades"]), len(b["trades"]))
        self.assertEqual(a["snapshots"][-1]["regime"], b["snapshots"][-1]["regime"])


if __name__ == "__main__":
    unittest.main()
