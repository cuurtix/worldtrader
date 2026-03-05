package com.worldtrader.api.market.secure.error;

public class MarketClosedError extends RuntimeException {
    public MarketClosedError(String message) { super(message); }
}
