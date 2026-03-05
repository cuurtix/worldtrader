import unittest

from worldtrader.orderbook.lob import MatchingEngine
from worldtrader.types import Order, OrderType, Side, TimeInForce


class MatchingTests(unittest.TestCase):
    def test_price_time_fifo(self):
        m = MatchingEngine()
        m.submit(Order("1", "A", "SPX", "XNYS", Side.SELL, 5, OrderType.LIMIT, TimeInForce.GTC, 101.0), 1)
        m.submit(Order("2", "B", "SPX", "XNYS", Side.SELL, 5, OrderType.LIMIT, TimeInForce.GTC, 101.0), 2)
        trades = m.submit(Order("3", "C", "SPX", "XNYS", Side.BUY, 7, OrderType.MARKET, TimeInForce.IOC), 3)
        self.assertEqual(sum(t.qty for t in trades), 7)
        self.assertEqual(trades[0].seller, "A")


if __name__ == "__main__":
    unittest.main()
