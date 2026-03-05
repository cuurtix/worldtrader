package com.worldtrader.api.market.secure.ws;

import com.worldtrader.api.market.secure.model.OrderModel;

public interface OrderWebSocketNotifier {
    void notifyOrderUpdate(OrderModel order);
}
