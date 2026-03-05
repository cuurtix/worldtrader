package com.worldtrader.api.market;

import com.worldtrader.api.market.flow.OrderSizeModel;
import com.worldtrader.api.market.flow.PoissonSampler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderFlowModelTest {

    @Test
    void poissonSamplerProducesNonNegativeCounts() {
        for (int i = 0; i < 100; i++) {
            assertTrue(PoissonSampler.sample(3.5) >= 0);
        }
    }

    @Test
    void heavyTailSizeCanProduceLargeOrders() {
        int max = 0;
        for (int i = 0; i < 400; i++) {
            max = Math.max(max, OrderSizeModel.sampleHeavyTailSize());
        }
        assertTrue(max > 80);
    }
}
