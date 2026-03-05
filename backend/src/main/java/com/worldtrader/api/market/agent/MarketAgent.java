package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;

import java.util.List;

public interface MarketAgent {
    List<OrderRequest> generate(AgentContext context);
}
