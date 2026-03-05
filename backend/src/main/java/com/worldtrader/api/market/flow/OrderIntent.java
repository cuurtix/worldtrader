package com.worldtrader.api.market.flow;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

public record OrderIntent(
        String traderId,
        String ticker,
        Side side,
        OrderType type,
        int qty,
        Double price,
        TimeInForce tif
) {}
