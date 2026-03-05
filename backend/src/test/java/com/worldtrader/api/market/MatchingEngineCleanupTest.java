package com.worldtrader.api.market;

import com.worldtrader.api.market.engine.MatchingEngine;
import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.engine.PriceTicks;
import com.worldtrader.api.market.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchingEngineCleanupTest {

    @Test
    void makerFullyFilledIsPurgedFromIndex() {
        PriceTicks ticks = new PriceTicks(0.01);
        OrderBook book = new OrderBook("AAPL");
        MatchingEngine engine = new MatchingEngine(ticks);

        Order maker = new Order("maker-1", "maker", "AAPL", Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 10, ticks.toTicks(100.0), Instant.parse("2025-01-20T15:30:00Z"));
        engine.execute(book, maker, Instant.parse("2025-01-20T15:30:00Z"));

        engine.execute(book, new Order("taker-1", "taker", "AAPL", Side.BUY, OrderType.MARKET, TimeInForce.IOC, 10, 0L, Instant.parse("2025-01-20T15:30:01Z")), Instant.parse("2025-01-20T15:30:01Z"));

        assertNull(book.getOrder("maker-1"));
        assertEquals(0, book.totalRestingOrders());
    }
}
