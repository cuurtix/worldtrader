package com.worldtrader.api.market.dto;

import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

public record OrderRequest(String traderId, String ticker, Side side, OrderType type, int qty, Double price, TimeInForce tif) {}
