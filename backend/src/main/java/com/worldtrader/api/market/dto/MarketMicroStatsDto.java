package com.worldtrader.api.market.dto;

public record MarketMicroStatsDto(
        long tick,
        long submittedOrders,
        long cancelRequests,
        long cancelSuccess,
        long totalTrades,
        double cancelToTradeRatio,
        double avgSpread,
        double avgTopDepth,
        double avgTradeSize,
        double lag1ReturnAutocorr,
        long totalRestingOrders
) {}
