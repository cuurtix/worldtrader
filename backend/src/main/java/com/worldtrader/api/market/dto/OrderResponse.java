package com.worldtrader.api.market.dto;

import com.worldtrader.api.market.model.OrderStatus;

import java.util.List;

public record OrderResponse(String orderId, OrderStatus status, List<FillDto> fills, int remainingQty) {}
