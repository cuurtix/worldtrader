package com.worldtrader.api.market;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;
import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketSimulationServiceDeterminismTest {

    private MarketSimulationService createService() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(0);
        props.setMmLevels(3);
        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();
        return service;
    }

    @Test
    void simulationClockAdvancesByInterval() {
        MarketSimulationService service = createService();
        service.setIntervalMillis(1000);
        Instant t0 = service.simulationTime();

        service.tick();
        service.tick();
        service.tick();

        assertEquals(t0.plusSeconds(3), service.simulationTime());
    }

    @Test
    void makerFilledOrderIsRemovedFromActiveTraderSet() {
        MarketSimulationService service = createService();
        service.submitOrder(new OrderRequest("maker", "AAPL", Side.SELL, OrderType.LIMIT, 10, 100.0, TimeInForce.GTC));
        assertEquals(1, service.countActiveOrdersForTrader("maker"));

        service.submitOrder(new OrderRequest("taker", "AAPL", Side.BUY, OrderType.MARKET, 10, null, TimeInForce.IOC));

        assertEquals(0, service.countActiveOrdersForTrader("maker"));
    }

    @Test
    void bestBidUsesTickOrdering() {
        MarketSimulationService service = createService();
        service.submitOrder(new OrderRequest("b1", "AAPL", Side.BUY, OrderType.LIMIT, 5, 100.10, TimeInForce.GTC));
        service.submitOrder(new OrderRequest("b2", "AAPL", Side.BUY, OrderType.LIMIT, 5, 100.11, TimeInForce.GTC));

        var book = service.getOrderBook("AAPL", 5);
        assertEquals(100.11, book.bestBid(), 1e-9);
    }
}
