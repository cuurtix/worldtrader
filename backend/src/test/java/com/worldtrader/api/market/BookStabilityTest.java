package com.worldtrader.api.market;

import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookStabilityTest {

    @Test
    void depthDoesNotExplodeWithCancelReplace() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(10);
        props.setMmLevels(5);

        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();
        service.updateRegime(service.getRegime());

        for (int i = 0; i < 180; i++) {
            service.tick();
        }

        long totalResting = service.getMicroStats().totalRestingOrders();
        assertTrue(totalResting < 80000, "resting orders should remain bounded");
    }
}
