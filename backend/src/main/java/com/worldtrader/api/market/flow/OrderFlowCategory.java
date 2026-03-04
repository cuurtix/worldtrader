package com.worldtrader.api.market.flow;

import java.util.List;

public interface OrderFlowCategory {
    double intensity(OrderFlowContext context, String ticker);
    List<OrderIntent> generateOrders(OrderFlowContext context, String ticker, double dtSeconds, int count);
}
