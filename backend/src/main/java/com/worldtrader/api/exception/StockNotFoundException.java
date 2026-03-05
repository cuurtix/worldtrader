package com.worldtrader.api.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(String ticker) {
        super("No stock with ticker symbol " + ticker + " exists");
    }
}
