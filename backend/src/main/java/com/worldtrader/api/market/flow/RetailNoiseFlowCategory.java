package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RetailNoiseFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context) {
        var m = context.metrics();
        double vol = Math.min(3.0, m.rv() * 10);
        return 2.0 + 4.0 * context.regime().getNewsIntensity() + 2.0 * context.regime().getBurstiness() + 0.6 * vol;
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics();
        double spreadNorm = (m.mid() == null || m.spread() == null) ? 0.0 : m.spread() / Math.max(m.mid(), 1e-6);
        double pMarket = 0.20 + 0.30 * context.regime().getNewsIntensity() + 0.35 * Math.min(1.0, spreadNorm * 800) + 0.2 * Math.min(1.0, m.rv() * 10);
        for (int i = 0; i < count; i++) {
            Side side = ThreadLocalRandom.current().nextBoolean() ? Side.BUY : Side.SELL;
            boolean market = ThreadLocalRandom.current().nextDouble() < pMarket;
            int qty = Math.max(1, Math.min(300, OrderSizeModel.sampleHeavyTailSize()));
            if (market) {
                out.add(new OrderIntent("FLOW_RETAIL", context.ticker(), side, OrderType.MARKET, qty, null, TimeInForce.IOC));
            } else {
                double ref = context.market().getLastPrice(context.ticker());
                double offset = ref * (0.0002 + ThreadLocalRandom.current().nextDouble() * 0.0015);
                double px = side == Side.BUY ? ref - offset : ref + offset;
                out.add(new OrderIntent("FLOW_RETAIL", context.ticker(), side, OrderType.LIMIT, qty, px, TimeInForce.GTC));
            }
        }
        return out;
    }
}
