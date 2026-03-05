package com.worldtrader.api.market.dto;

import java.time.Instant;
import java.util.List;

public record OrderBookSnapshot(String ticker, List<Level> bids, List<Level> asks, Instant ts) {
    public record Level(double price, int totalQty, int orderCount) {}
}
