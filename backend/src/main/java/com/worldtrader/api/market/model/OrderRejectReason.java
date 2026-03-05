package com.worldtrader.api.market.model;

public enum OrderRejectReason {
    NONE,
    INVALID_ORDER,
    UNKNOWN_TICKER,
    INSUFFICIENT_CASH,
    INSUFFICIENT_POSITION,
    INSUFFICIENT_LIQUIDITY,
    FOK_NOT_FILLED,
    RISK_REJECT
}
