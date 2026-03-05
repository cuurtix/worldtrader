package com.worldtrader.api.market.secure.error;

public class InvalidOrderError extends RuntimeException {
    public InvalidOrderError(String message) { super(message); }
}
