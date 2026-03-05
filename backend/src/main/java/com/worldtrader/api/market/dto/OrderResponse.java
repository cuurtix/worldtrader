package com.worldtrader.api.market.dto;

import com.worldtrader.api.market.model.OrderStatus;
import com.worldtrader.api.market.model.OrderRejectReason;

import java.util.List;

public record OrderResponse(String orderId,
                            OrderStatus status,
                            List<FillDto> fills,
                            int remainingQty,
                            int filledQty,
                            int remainingCanceledQty,
                            OrderRejectReason reason) {}
