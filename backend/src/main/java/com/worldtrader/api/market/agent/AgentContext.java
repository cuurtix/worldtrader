package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.model.MarketRegime;
import com.worldtrader.api.market.service.MarketSimulationService;

public record AgentContext(MarketRegime regime, MarketSimulationService market) {}
