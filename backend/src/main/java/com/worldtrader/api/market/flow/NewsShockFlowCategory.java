package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NewsShockFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context) {
        var r = context.regime();
        return 0.2 + 8.0 * r.getNewsIntensity() + 4.0 * r.getPoliticalStress() + 3.0 * r.getBurstiness();
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics();
        double directionalBias = Math.tanh(1.2 * (context.regime().getRiskOnOff() - context.regime().getPoliticalStress()));
        for (int i = 0; i < count; i++) {
            Side side = ThreadLocalRandom.current().nextDouble() < (0.5 + 0.35 * directionalBias) ? Side.BUY : Side.SELL;
            int qty = Math.max(10, Math.min(2000, (int) Math.round(OrderSizeModel.sampleHeavyTailSize() * (1.0 + 2.0 * context.regime().getNewsIntensity()))));
            boolean ioc = ThreadLocalRandom.current().nextDouble() < 0.8;
            out.add(new OrderIntent("FLOW_NEWS", context.ticker(), side, OrderType.MARKET, qty, null, ioc ? TimeInForce.IOC : TimeInForce.GTC));
        }
        return out;
    }
}
