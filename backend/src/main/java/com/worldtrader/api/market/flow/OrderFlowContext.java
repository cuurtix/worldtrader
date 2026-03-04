package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.dto.MetricsDto;
import com.worldtrader.api.market.model.MarketRegime;
import com.worldtrader.api.market.service.MarketSimulationService;

public record OrderFlowContext(MarketRegime regime, MarketSimulationService market, String ticker) {
    public MetricsDto metrics() {
        return market.getMetrics(ticker);
    }
}
