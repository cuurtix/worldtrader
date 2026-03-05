package com.worldtrader.api.market;

import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiMarketMakerTest {

    @Test
    void configuredNumberOfMarketMakersIsActive() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(7);
        props.setMmLevels(4);
        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();
        assertEquals(7, service.getMarketMakerCount());
    }

    @Test
    void maxActiveOrdersPerMmIsEnforced() {
        MarketSimulationProperties props = new MarketSimulationProperties();
        props.setMarketMakers(6);
        props.setMmLevels(6);
        props.setMaxActiveOrdersPerMmPerTicker(24);
        MarketSimulationService service = new MarketSimulationService(props);
        service.bootstrap();

        for (int i = 0; i < 120; i++) {
            service.tick();
        }

        for (int i = 0; i < service.getMarketMakerCount(); i++) {
            int active = service.countActiveOrdersForTrader("MM_" + i);
            assertTrue(active <= props.getMaxActiveOrdersPerMmPerTicker() * 3, "active per MM should remain bounded by ticker universe");
        }
    }
}
