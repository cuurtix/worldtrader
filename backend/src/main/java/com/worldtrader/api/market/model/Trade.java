package com.worldtrader.api.market.model;

import java.time.Instant;

public record Trade(
        String tradeId,
        String ticker,
        double price,
        int qty,
        Side aggressorSide,
        Instant timestamp,
        String buyOrderId,
        String sellOrderId,
        String buyerTraderId,
        String sellerTraderId
) {}
