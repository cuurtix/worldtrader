package com.worldtrader.api.market;

import com.worldtrader.api.market.engine.MatchingEngine;
import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    @Test
    void limitAndMarketPartialFillWorks() {
        OrderBook book = new OrderBook("AAPL");
        MatchingEngine engine = new MatchingEngine();

        engine.execute(book, new Order("s1", "seller1", "AAPL", Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 10, 100.0, Instant.now()));
        List<Trade> trades = engine.execute(book, new Order("b1", "buyer1", "AAPL", Side.BUY, OrderType.MARKET, TimeInForce.IOC, 6, null, Instant.now()));

        assertEquals(1, trades.size());
        assertEquals(6, trades.get(0).qty());
        assertEquals(4, book.asks().firstEntry().getValue().totalQty());
    }

    @Test
    void fifoAtSamePriceIsRespected() {
        OrderBook book = new OrderBook("AAPL");
        MatchingEngine engine = new MatchingEngine();
        engine.execute(book, new Order("s1", "s1", "AAPL", Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 5, 101.0, Instant.now()));
        engine.execute(book, new Order("s2", "s2", "AAPL", Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 5, 101.0, Instant.now()));

        List<Trade> trades = engine.execute(book, new Order("b1", "b", "AAPL", Side.BUY, OrderType.MARKET, TimeInForce.IOC, 7, null, Instant.now()));
        assertEquals("s1", trades.get(0).sellerTraderId());
    }
}
