package com.worldtrader.api.market;

import com.worldtrader.api.market.service.MarketSimulationProperties;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioCashOpsTest {

    @Test
    void depositAndWithdrawAdjustCash() {
        MarketSimulationService service = new MarketSimulationService(new MarketSimulationProperties());
        service.bootstrap();

        double initialCash = service.getPortfolio("retail").cash();
        double afterDeposit = service.deposit("retail", 5000).cash();
        double afterWithdraw = service.withdraw("retail", 7500).cash();

        assertEquals(initialCash + 5000, afterDeposit, 1e-9);
        assertEquals(initialCash - 2500, afterWithdraw, 1e-9);
    }
}
