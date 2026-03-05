import unittest

from worldtrader.realtime.models import Order, OrderType, Side
from worldtrader.realtime.orderbook import LimitOrderBook


class RealTimeOrderBookTest(unittest.TestCase):
    def test_partial_fill_and_time_priority(self):
        book = LimitOrderBook("AAPL")
        book.submit(Order("b1", "maker1", "AAPL", Side.BUY, 10, 1.0, OrderType.LIMIT, 100.0))
        book.submit(Order("b2", "maker2", "AAPL", Side.BUY, 10, 2.0, OrderType.LIMIT, 100.0))
        trades = book.submit(Order("s1", "taker", "AAPL", Side.SELL, 15, 3.0, OrderType.MARKET))

        self.assertEqual(2, len(trades))
        self.assertEqual("maker1", trades[0].buy_agent)
        self.assertEqual(10, trades[0].size)
        self.assertEqual("maker2", trades[1].buy_agent)
        self.assertEqual(5, trades[1].size)

    def test_cancel_removes_order_and_rank(self):
        book = LimitOrderBook("BTC")
        book.submit(Order("x1", "a", "BTC", Side.SELL, 7, 1.0, OrderType.LIMIT, 60010.0))
        self.assertEqual(1, book.queue_rank("x1"))
        self.assertTrue(book.cancel("x1"))
        self.assertIsNone(book.queue_rank("x1"))


if __name__ == "__main__":
    unittest.main()
