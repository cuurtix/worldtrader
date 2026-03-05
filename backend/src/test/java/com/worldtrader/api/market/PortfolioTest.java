package com.worldtrader.api.market;

import com.worldtrader.api.market.model.Portfolio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioTest {
    @Test
    void avgCostAndRealizedPnlAreCorrect() {
        Portfolio p = new Portfolio("T1", 10000);
        p.applyBuy("AAPL", 10, 100);
        p.applyBuy("AAPL", 10, 120);
        assertEquals(2200, 10000 - p.cash(), 1e-9);
        assertEquals(0, p.realizedPnl(), 1e-9);
        p.applySell("AAPL", 5, 130);
        assertEquals(100, p.realizedPnl(), 1e-9);
    }
}
