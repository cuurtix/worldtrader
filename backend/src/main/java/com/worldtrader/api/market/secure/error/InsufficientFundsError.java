package com.worldtrader.api.market.secure.error;

public class InsufficientFundsError extends RuntimeException {
    public InsufficientFundsError(String message) { super(message); }
}
