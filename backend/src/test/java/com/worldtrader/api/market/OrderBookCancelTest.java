package com.worldtrader.api.market;

import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.model.Order;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookCancelTest {
    @Test
    void cancelRemovesOrderFromBook() {
        OrderBook b = new OrderBook("AAPL");
        Order o = new Order("oid-1", "MM_0", "AAPL", Side.BUY, OrderType.LIMIT, TimeInForce.GTC, 15, 100.0, Instant.now());
        b.add(o);
        assertTrue(b.cancel("oid-1"));
        assertNull(b.bestBid());
    }
}
