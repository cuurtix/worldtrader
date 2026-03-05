package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LiquidationFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context) {
        double stress = context.regime().getPoliticalStress() + (1.0 - context.regime().getLiquidity());
        return Math.max(0.0, -0.2 + 2.5 * stress + 1.5 * context.regime().getNewsIntensity());
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics();
        for (int i = 0; i < count; i++) {
            Side side = ThreadLocalRandom.current().nextDouble() < (0.55 + 0.35 * context.regime().getPoliticalStress()) ? Side.SELL : Side.BUY;
            int qty = Math.max(8, Math.min(1500, (int) Math.round(OrderSizeModel.sampleHeavyTailSize() * (1.0 + context.regime().getPoliticalStress()))));
            out.add(new OrderIntent("FLOW_LIQ", context.ticker(), side, OrderType.MARKET, qty, null, TimeInForce.IOC));
        }
        return out;
    }
}
