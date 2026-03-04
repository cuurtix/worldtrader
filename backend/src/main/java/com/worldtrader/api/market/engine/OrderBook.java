package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.Order;
import com.worldtrader.api.market.model.PriceLevel;
import com.worldtrader.api.market.model.Side;

import java.util.*;

public class OrderBook {
    private final String ticker;
    private final NavigableMap<Double, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Double, PriceLevel> asks = new TreeMap<>();

    public OrderBook(String ticker) { this.ticker = ticker; }
    public String ticker() { return ticker; }
    public NavigableMap<Double, PriceLevel> bids() { return bids; }
    public NavigableMap<Double, PriceLevel> asks() { return asks; }

    public Double bestBid() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Double bestAsk() { return asks.isEmpty() ? null : asks.firstKey(); }
    public Double spread() { return (bestBid()==null||bestAsk()==null)?null:bestAsk()-bestBid(); }
    public Double mid() { return (bestBid()==null||bestAsk()==null)?null:(bestAsk()+bestBid())/2.0; }

    public void add(Order o) {
        var sideMap = o.side() == Side.BUY ? bids : asks;
        var level = sideMap.computeIfAbsent(o.limitPrice(), PriceLevel::new);
        level.add(o);
    }

    public boolean cancel(String orderId) {
        return cancelInMap(orderId, bids) || cancelInMap(orderId, asks);
    }

    private boolean cancelInMap(String orderId, NavigableMap<Double, PriceLevel> map) {
        for (var entry : new ArrayList<>(map.entrySet())) {
            var level = entry.getValue();
            Iterator<Order> it = level.queue().iterator();
            while (it.hasNext()) {
                Order o = it.next();
                if (o.orderId().equals(orderId)) {
                    level.reduce(o.remainingQty());
                    it.remove();
                    if (level.empty()) map.remove(entry.getKey());
                    return true;
                }
            }
        }
        return false;
    }

    public int depthQty(Side side, int levels) {
        NavigableMap<Double, PriceLevel> map = side == Side.BUY ? bids : asks;
        return map.values().stream().limit(levels).mapToInt(PriceLevel::totalQty).sum();
    }
}
