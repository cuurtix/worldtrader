package com.worldtrader.api.market.model;

import java.time.Instant;

public class Order {
    private final String orderId;
    private final String traderId;
    private final String ticker;
    private final Side side;
    private final OrderType type;
    private final TimeInForce tif;
    private final int quantity;
    private int remainingQty;
    private final long priceTicks;
    private final Instant timestamp;

    public Order(String orderId, String traderId, String ticker, Side side, OrderType type, TimeInForce tif, int quantity, long priceTicks, Instant timestamp) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.ticker = ticker;
        this.side = side;
        this.type = type;
        this.tif = tif;
        this.quantity = quantity;
        this.remainingQty = quantity;
        this.priceTicks = priceTicks;
        this.timestamp = timestamp;
    }

    public String orderId() { return orderId; }
    public String traderId() { return traderId; }
    public String ticker() { return ticker; }
    public Side side() { return side; }
    public OrderType type() { return type; }
    public TimeInForce tif() { return tif; }
    public int quantity() { return quantity; }
    public int remainingQty() { return remainingQty; }
    public long priceTicks() { return priceTicks; }
    public Instant timestamp() { return timestamp; }
    public void decreaseRemaining(int filled) { this.remainingQty -= filled; }
}
