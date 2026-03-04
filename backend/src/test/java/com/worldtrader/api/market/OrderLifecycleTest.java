package com.worldtrader.api.market;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;
import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderLifecycleTest {

    @Test
    void cancelReplaceDoesNotLeaveOrphansInActiveSet() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(5);
        props.setMmLevels(3);
        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();

        var resp = service.submitOrder(new OrderRequest("MM_0", "AAPL", Side.BUY, OrderType.LIMIT, 20, 180.0, TimeInForce.GTC));
        assertTrue(service.countActiveOrdersForTrader("MM_0") >= 1);
        service.cancelOrder(resp.orderId());
        assertEquals(0, service.countActiveOrdersForTrader("MM_0"));
    }
}
