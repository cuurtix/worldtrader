package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ArbFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context) {
        var m = context.metrics();
        double dislocation = Math.abs(m.nofi()) + Math.abs(m.li());
        return 0.3 + 1.8 * dislocation + 0.6 * context.regime().getBurstiness();
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics();
        if (m.mid() == null) return out;
        for (int i = 0; i < count; i++) {
            Side side = m.li() > 0 ? Side.SELL : Side.BUY;
            int qty = Math.max(1, Math.min(400, OrderSizeModel.sampleHeavyTailSize()));
            double px = side == Side.BUY ? m.mid() * 1.0003 : m.mid() * 0.9997; // marketable limit IOC
            out.add(new OrderIntent("FLOW_ARB", context.ticker(), side, OrderType.LIMIT, qty, px, TimeInForce.IOC));
        }
        return out;
    }
}
