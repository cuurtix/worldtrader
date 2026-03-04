package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;

public class MeanReversionFlowCategory implements OrderFlowCategory {
    @Override
    public double intensity(OrderFlowContext context, String ticker) {
        return 0.7 + 1.5 * (1.0 - context.regime().getLiquidity());
    }

    @Override
    public List<OrderIntent> generateOrders(OrderFlowContext context, String ticker, double dtSeconds, int count) {
        List<OrderIntent> out = new ArrayList<>();
        var m = context.metrics(ticker);
        if (m.mid() == null) return out;
        double delta = 0.0015;
        for (int i = 0; i < count; i++) {
            if (m.mid() > m.vwap() * (1 + delta)) {
                out.add(new OrderIntent("FLOW_MR", ticker, Side.SELL, OrderType.LIMIT, OrderSizeModel.sampleHeavyTailSize(), m.mid() * 1.0002, TimeInForce.GTC));
            } else if (m.mid() < m.vwap() * (1 - delta)) {
                out.add(new OrderIntent("FLOW_MR", ticker, Side.BUY, OrderType.LIMIT, OrderSizeModel.sampleHeavyTailSize(), m.mid() * 0.9998, TimeInForce.GTC));
            }
        }
        return out;
    }
}
