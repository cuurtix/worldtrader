package com.worldtrader.api.market.model;

import java.util.ArrayDeque;
import java.util.Deque;

public class PriceLevel {
    private final long priceTicks;
    private final Deque<Order> queue = new ArrayDeque<>();
    private int totalQty = 0;

    public PriceLevel(long priceTicks) { this.priceTicks = priceTicks; }
    public long priceTicks() { return priceTicks; }
    public int totalQty() { return totalQty; }
    public Deque<Order> queue() { return queue; }

    public void add(Order order) { queue.addLast(order); totalQty += order.remainingQty(); }
    public void reduce(int qty) { totalQty -= qty; }
    public boolean empty() { return totalQty <= 0 || queue.isEmpty(); }
}
