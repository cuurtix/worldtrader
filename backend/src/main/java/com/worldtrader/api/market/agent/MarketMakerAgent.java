package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;

public class MarketMakerAgent implements MarketAgent {
    @Override
    public List<OrderRequest> generate(AgentContext context) {
        List<OrderRequest> out = new ArrayList<>();
        var regime = context.regime();
        for (String t : context.market().tickers()) {
            double mid = context.market().getLastPrice(t);
            double rv = context.market().getMetrics(t).rv();
            double spread = Math.max(0.01, mid * 0.0008 * (1 + 2 * rv + 1.5 * regime.getPoliticalStress()) / Math.max(0.05, regime.getLiquidity()));
            out.add(new OrderRequest("MM", t, Side.BUY, OrderType.LIMIT, 25, mid - spread / 2, TimeInForce.GTC));
            out.add(new OrderRequest("MM", t, Side.SELL, OrderType.LIMIT, 25, mid + spread / 2, TimeInForce.GTC));
        }
        return out;
    }
}
