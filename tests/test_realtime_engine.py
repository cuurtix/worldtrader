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

    def test_market_cap_updates_with_price(self):
        market = RealTimeMarket()
        symbol = "AAPL"
        st = market.asset_state[symbol]
        old_shares = st.shares_outstanding
        market._set_price(symbol, 250.0)
        self.assertEqual(250.0 * old_shares, market.asset_state[symbol].market_cap)

    def test_split_keeps_market_cap_coherent(self):
        market = RealTimeMarket()
        symbol = "AAPL"
        before = market.asset_state[symbol].market_cap
        market._apply_split(symbol, 2.0)
        after = market.asset_state[symbol].market_cap
        self.assertAlmostEqual(before, after, delta=max(1.0, before * 1e-9))

    def test_candles_are_available_for_multiple_timeframes(self):
        market = RealTimeMarket()
        symbol = "AAPL"
        now = 1000.0
        market._update_candles(symbol, now, 100.0, 2)
        market._update_candles(symbol, now + 1, 101.0, 1)
        market._update_candles(symbol, now + 6, 99.0, 3)
        c1 = market.get_candles(symbol, tf=1, limit=10)
        c5 = market.get_candles(symbol, tf=5, limit=10)
        self.assertTrue(len(c1) >= 2)
        self.assertTrue(len(c5) >= 2)

if __name__ == "__main__":
    unittest.main()
