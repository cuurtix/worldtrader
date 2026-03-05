import asyncio
import unittest

from worldtrader.realtime.engine import Event, RealTimeMarket
from worldtrader.realtime.models import Order, OrderType, Side


class RealTimeEngineTest(unittest.TestCase):
    def test_cancel_random_does_not_crash_when_levels_shrink(self):
        market = RealTimeMarket()
        symbol = "AAPL"
        for i in range(5):
            market.books[symbol].submit(
                Order(f"extra-{i}", "agent", symbol, Side.BUY, 1, 1.0 + i, OrderType.LIMIT, 150.0 + i)
            )
        market._cancel_random(symbol)

    def test_priority_queue_tie_breaker_same_timestamp(self):
        market = RealTimeMarket()

        async def scenario():
            await market.enqueue_event(Event("agent_decision", {}, 123.0))
            await market.enqueue_event(Event("agent_decision", {}, 123.0))
            a = await market.event_q.get()
            b = await market.event_q.get()
            self.assertEqual(123.0, a[0])
            self.assertEqual(123.0, b[0])
            self.assertLess(a[1], b[1])

        asyncio.run(scenario())


if __name__ == "__main__":
    unittest.main()
