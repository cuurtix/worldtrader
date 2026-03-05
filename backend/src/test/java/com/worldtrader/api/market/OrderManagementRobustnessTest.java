package com.worldtrader.api.market;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderRejectReason;
import com.worldtrader.api.market.model.OrderStatus;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;
import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderManagementRobustnessTest {

    private MarketSimulationService createService() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(0);
        props.setMmLevels(2);
        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();
        return service;
    }

    @Test
    void iocCancelsRemainderWithoutResting() {
        MarketSimulationService service = createService();
        service.submitOrder(new OrderRequest("SEED_MM", "AAPL", Side.SELL, OrderType.LIMIT, 10, 100.0, TimeInForce.GTC));

        var response = service.submitOrder(new OrderRequest("BUYER", "AAPL", Side.BUY, OrderType.LIMIT, 15, 100.0, TimeInForce.IOC));

        assertEquals(OrderStatus.PARTIALLY_FILLED, response.status());
        assertEquals(10, response.filledQty());
        assertEquals(5, response.remainingCanceledQty());
        assertEquals(0, service.countActiveOrdersForTrader("BUYER"));
    }

    @Test
    void fokRejectsWhenNotFullyExecutable() {
        MarketSimulationService service = createService();

        var response = service.submitOrder(new OrderRequest("BUYER", "AAPL", Side.BUY, OrderType.LIMIT, 5000, 100.0, TimeInForce.FOK));

        assertEquals(OrderStatus.EXPIRED, response.status());
        assertEquals(OrderRejectReason.FOK_NOT_FILLED, response.reason());
        assertTrue(response.fills().isEmpty());
    }

    @Test
    void selfTradePreventionSkipsOwnMakerOrders() {
        MarketSimulationService service = createService();
        // trader gets inventory first
        service.submitOrder(new OrderRequest("TR", "AAPL", Side.BUY, OrderType.LIMIT, 20, 190.15, TimeInForce.IOC));
        service.submitOrder(new OrderRequest("TR", "AAPL", Side.SELL, OrderType.LIMIT, 10, 100.0, TimeInForce.GTC));

        var response = service.submitOrder(new OrderRequest("TR", "AAPL", Side.BUY, OrderType.LIMIT, 10, 100.0, TimeInForce.IOC));

        assertTrue(response.fills().isEmpty());
    }

    @Test
    void riskRejectsBuyWithoutCashAndSellWithoutPosition() {
        MarketSimulationService service = createService();

        var noCash = service.submitOrder(new OrderRequest("R1", "AAPL", Side.BUY, OrderType.LIMIT, 1_000_000, 100.0, TimeInForce.GTC));
        assertEquals(OrderStatus.REJECTED, noCash.status());
        assertEquals(OrderRejectReason.INSUFFICIENT_CASH, noCash.reason());

        var noPos = service.submitOrder(new OrderRequest("R2", "AAPL", Side.SELL, OrderType.LIMIT, 10, 100.0, TimeInForce.GTC));
        assertEquals(OrderStatus.REJECTED, noPos.status());
        assertEquals(OrderRejectReason.INSUFFICIENT_POSITION, noPos.reason());
    }

    @SuppressWarnings("unchecked")
    @Test
    void cancelUsesLocatorAndDoesNotFallbackScan() throws Exception {
        MarketSimulationService service = createService();
        var order = service.submitOrder(new OrderRequest("SEED_MM", "AAPL", Side.BUY, OrderType.LIMIT, 10, 100.0, TimeInForce.GTC));

        Field locatorField = MarketSimulationService.class.getDeclaredField("orderLocator");
        locatorField.setAccessible(true);
        Map<String, ?> locator = (Map<String, ?>) locatorField.get(service);
        locator.remove(order.orderId());

        assertFalse(service.cancelOrder(order.orderId()));
        assertEquals(1, service.countActiveOrdersForTrader("SEED_MM"));
    }
}
