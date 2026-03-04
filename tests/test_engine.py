import unittest

from worldtrader.engine import MarketEngine
from worldtrader.models import Order, Portfolio, Side, Stock


class TestMarketEngine(unittest.TestCase):
    def setUp(self) -> None:
        self.engine = MarketEngine([Stock("ACME", 100.0)], fee_rate=0.0)
        self.engine.register_trader("buyer", Portfolio(cash=1000.0, positions={"ACME": 0}))
        self.engine.register_trader("seller", Portfolio(cash=1000.0, positions={"ACME": 10}))

    def test_simple_match(self):
        buy = Order("buyer", "ACME", Side.BUY, 5, 101.0, 1)
        sell = Order("seller", "ACME", Side.SELL, 5, 99.0, 1)

        self.engine.submit_order(buy, tick=1)
        trades = self.engine.submit_order(sell, tick=1)

        self.assertEqual(len(trades), 1)
        self.assertEqual(self.engine.portfolios["buyer"].positions["ACME"], 5)
        self.assertEqual(self.engine.portfolios["seller"].positions["ACME"], 5)

    def test_price_time_priority(self):
        b1 = Order("buyer", "ACME", Side.BUY, 2, 101.0, 1)
        b2 = Order("buyer", "ACME", Side.BUY, 2, 102.0, 2)
        s1 = Order("seller", "ACME", Side.SELL, 2, 100.0, 3)

        self.engine.submit_order(b1, tick=1)
        self.engine.submit_order(b2, tick=2)
        trades = self.engine.submit_order(s1, tick=3)

        self.assertEqual(len(trades), 1)
        self.assertEqual(trades[0].quantity, 2)


if __name__ == "__main__":
    unittest.main()
