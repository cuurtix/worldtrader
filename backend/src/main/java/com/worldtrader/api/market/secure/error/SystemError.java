package com.worldtrader.api.market.secure.error;

public class SystemError extends RuntimeException {
    public SystemError(String message, Throwable cause) { super(message, cause); }
}
