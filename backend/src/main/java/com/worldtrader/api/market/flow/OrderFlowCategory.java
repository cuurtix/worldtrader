package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.dto.OrderRequest;

import java.util.List;

public interface OrderFlowCategory {
    double intensity(OrderFlowContext context);
    List<OrderIntent> generateOrders(OrderFlowContext context, double dtSeconds, int count);

    default OrderRequest buildOrder(OrderIntent intent) {
        return new OrderRequest(intent.traderId(), intent.ticker(), intent.side(), intent.type(), intent.qty(), intent.price(), intent.tif());
    }
}
