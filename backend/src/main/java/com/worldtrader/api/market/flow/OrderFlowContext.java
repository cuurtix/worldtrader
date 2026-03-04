package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.dto.MetricsDto;
import com.worldtrader.api.market.model.MarketRegime;
import com.worldtrader.api.market.service.MarketSimulationService;

public record OrderFlowContext(MarketRegime regime, MarketSimulationService market) {
    public MetricsDto metrics(String ticker) {
        return market.getMetrics(ticker);
    }
}
