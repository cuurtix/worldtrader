import unittest

from worldtrader.correlation.model import CorrelationModel


class CorrelationTests(unittest.TestCase):
    def test_stress_raises_corr(self):
        model = CorrelationModel(base_corr=0.2)
        low = model.matrix(["A", "B"], stress=0.1)["A"]["B"]
        high = model.matrix(["A", "B"], stress=1.0)["A"]["B"]
        self.assertGreater(high, low)


if __name__ == "__main__":
    unittest.main()
