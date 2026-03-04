import unittest

from worldtrader.agents.base import RetailAgent
from worldtrader.risk.engine import RiskManager
from worldtrader.types import Instrument, AssetClass, Portfolio, RiskLimits, MarginAccount, Position


class RiskTests(unittest.TestCase):
    def test_liquidation_trigger(self):
        inst = Instrument("SPX", AssetClass.EQUITY, "XNYS", "USD", 0.01, 1, 100, 99.9, 100.1, 100)
        agent = RetailAgent("R", 1, RiskLimits(5, 0.5, 0.4), Portfolio("R", "USD", margin=MarginAccount(cash=-2000, equity=-2000)), "retail")
        agent.portfolio.positions["SPX"] = Position("SPX", qty=10, avg_price=120)
        rm = RiskManager(maintenance_margin=0.25)
        rm.update_equity(agent, {"SPX": inst})
        orders = rm.liquidation_orders(agent, {"SPX": inst}, 10)
        self.assertTrue(len(orders) >= 1)


if __name__ == "__main__":
    unittest.main()
