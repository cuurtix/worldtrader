package com.worldtrader.api.market.dto;

public record MarketMicroStatsDto(
        long tick,
        long submittedOrders,
        long cancelRequests,
        long cancelSuccess,
        long totalTrades,
        double cancelToSubmitRatio,
        double cancelToTradeRatio,
        double spreadMean,
        double spreadP95,
        double avgTopDepth,
        double tradeSizeMean,
        double tradeSizeP95,
        double lag1ReturnAutocorr,
        double ofiReturnCorr,
        double volatilityClustering,
        long totalRestingOrders
) {}
