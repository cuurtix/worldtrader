package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MomentumAgent implements MarketAgent {
    @Override
    public List<OrderRequest> generate(AgentContext context) {
        List<OrderRequest> out = new ArrayList<>();
        for (String t : context.market().tickers()) {
            var m = context.market().getMetrics(t);
            double z = m.nofi() + context.regime().getRiskOnOff() - (m.spread() == null ? 0.0 : m.spread() / Math.max(1e-6, m.mid()));
            double pBuy = 1.0 / (1.0 + Math.exp(-3 * z));
            double panic = 1.0 / (1.0 + Math.exp(-3 * (-z + context.regime().getPoliticalStress() + (1 - context.regime().getLiquidity()))));
            if (ThreadLocalRandom.current().nextDouble() < pBuy * 0.2) out.add(new OrderRequest("MOMO", t, Side.BUY, OrderType.MARKET, ThreadLocalRandom.current().nextInt(2, 12), null, TimeInForce.IOC));
            if (ThreadLocalRandom.current().nextDouble() < panic * 0.2) out.add(new OrderRequest("MOMO", t, Side.SELL, OrderType.MARKET, ThreadLocalRandom.current().nextInt(2, 12), null, TimeInForce.IOC));
        }
        return out;
    }
}
