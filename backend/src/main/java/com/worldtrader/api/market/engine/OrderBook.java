package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.Order;
import com.worldtrader.api.market.model.PriceLevel;
import com.worldtrader.api.market.model.Side;

import java.util.*;

public class OrderBook {
    private record OrderLocator(Side side, double price, Order ref) {}

    private final String ticker;
    private final NavigableMap<Double, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Double, PriceLevel> asks = new TreeMap<>();
    private final Map<String, OrderLocator> orderIndex = new HashMap<>();

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
        orderIndex.put(o.orderId(), new OrderLocator(o.side(), o.limitPrice(), o));
    }

    public boolean cancel(String orderId) {
        OrderLocator loc = orderIndex.get(orderId);
        if (loc != null) {
            NavigableMap<Double, PriceLevel> map = loc.side() == Side.BUY ? bids : asks;
            PriceLevel level = map.get(loc.price());
            if (level == null) {
                orderIndex.remove(orderId);
                return false;
            }
            boolean removed = level.queue().remove(loc.ref());
            if (removed) {
                level.reduce(loc.ref().remainingQty());
                if (level.empty()) map.remove(loc.price());
                orderIndex.remove(orderId);
                return true;
            }
            orderIndex.remove(orderId);
            return false;
        }
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
                    orderIndex.remove(orderId);
                    if (level.empty()) map.remove(entry.getKey());
                    return true;
                }
            }
        }
        return false;
    }

    public void removeIndex(String orderId) {
        orderIndex.remove(orderId);
    }

    public List<Order> restingOrders() {
        List<Order> out = new ArrayList<>();
        bids.values().forEach(l -> out.addAll(l.queue()));
        asks.values().forEach(l -> out.addAll(l.queue()));
        return out;
    }

    public int depthQty(Side side, int levels) {
        NavigableMap<Double, PriceLevel> map = side == Side.BUY ? bids : asks;
        return map.values().stream().limit(levels).mapToInt(PriceLevel::totalQty).sum();
    }

    public int totalRestingOrders() {
        return orderIndex.size();
    }
}
