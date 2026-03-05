package com.worldtrader.api.model;

import java.time.Instant;

public record StockPriceResponse(String ticker, double price, Instant time) {}
