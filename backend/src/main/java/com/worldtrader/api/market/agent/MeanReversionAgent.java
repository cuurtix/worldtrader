package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;

public class MeanReversionAgent implements MarketAgent {
    @Override
    public List<OrderRequest> generate(AgentContext context) {
        List<OrderRequest> out = new ArrayList<>();
        for (String t : context.market().tickers()) {
            var m = context.market().getMetrics(t);
            double delta = 0.002;
            if (m.mid() == null) continue;
            if (m.mid() > m.vwap() * (1 + delta)) out.add(new OrderRequest("MR", t, Side.SELL, OrderType.LIMIT, 8, m.mid() * 1.0005, TimeInForce.IOC));
            else if (m.mid() < m.vwap() * (1 - delta)) out.add(new OrderRequest("MR", t, Side.BUY, OrderType.LIMIT, 8, m.mid() * 0.9995, TimeInForce.IOC));
        }
        return out;
    }
}
