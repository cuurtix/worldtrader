package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MomentumFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context, String ticker) {
        var m = context.metrics(ticker);
        return 0.8 + 4.0 * Math.abs(m.nofi()) + 2.0 * context.regime().getBurstiness();
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, String ticker, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics(ticker);
        double z = m.nofi() + 0.8 * context.regime().getRiskOnOff();
        double pBuy = 1.0 / (1.0 + Math.exp(-3.5 * z));
        double pSellPanic = 1.0 / (1.0 + Math.exp(-3.5 * (-z + context.regime().getPoliticalStress() + (1 - context.regime().getLiquidity()))));
        for (int i = 0; i < count; i++) {
            double r = ThreadLocalRandom.current().nextDouble();
            Side side = r < pBuy ? Side.BUY : (r > 1.0 - pSellPanic ? Side.SELL : (ThreadLocalRandom.current().nextBoolean()?Side.BUY:Side.SELL));
            int qty = OrderSizeModel.sampleHeavyTailSize();
            out.add(new OrderIntent("FLOW_MOMO", ticker, side, OrderType.MARKET, qty, null, TimeInForce.IOC));
        }
        return out;
    }
}
