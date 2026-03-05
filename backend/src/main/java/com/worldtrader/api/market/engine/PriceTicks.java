package com.worldtrader.api.market.engine;

public final class PriceTicks {
    private final double tickSize;

    public PriceTicks(double tickSize) {
        this.tickSize = tickSize;
    }

    public long toTicks(double price) {
        return Math.round(price / tickSize);
    }

    public double toPrice(long ticks) {
        return ticks * tickSize;
    }

    public double tickSize() {
        return tickSize;
    }
}
