package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NoiseTraderAgent implements MarketAgent {
    @Override
    public List<OrderRequest> generate(AgentContext context) {
        List<OrderRequest> out = new ArrayList<>();
        double lambda = 0.4 + context.regime().getNewsIntensity() * 0.8;
        for (String t : context.market().tickers()) {
            if (ThreadLocalRandom.current().nextDouble() < lambda) {
                Side side = ThreadLocalRandom.current().nextBoolean() ? Side.BUY : Side.SELL;
                out.add(new OrderRequest("NOISE", t, side, OrderType.MARKET, ThreadLocalRandom.current().nextInt(1, 8), null, TimeInForce.IOC));
            }
        }
        return out;
    }
}
