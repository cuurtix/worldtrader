import unittest

from worldtrader.simulation import run_simulation


class TestSimulation(unittest.TestCase):
    def test_deterministic_seed(self):
        a = run_simulation(ticks=10, seed=123)
        b = run_simulation(ticks=10, seed=123)

        self.assertEqual(a["stocks"], b["stocks"])
        self.assertEqual(len(a["trades"]), len(b["trades"]))


if __name__ == "__main__":
    unittest.main()
