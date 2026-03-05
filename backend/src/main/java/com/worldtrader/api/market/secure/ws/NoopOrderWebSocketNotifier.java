package com.worldtrader.api.market.secure.ws;

import com.worldtrader.api.market.secure.model.OrderModel;
import org.springframework.stereotype.Component;

@Component
public class NoopOrderWebSocketNotifier implements OrderWebSocketNotifier {
    @Override
    public void notifyOrderUpdate(OrderModel order) {
        // plug actual websocket broker here
    }
}
