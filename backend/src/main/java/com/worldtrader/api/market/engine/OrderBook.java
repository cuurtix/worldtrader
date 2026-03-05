package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.dto.OrderBookSnapshot;
import com.worldtrader.api.market.model.Order;
import com.worldtrader.api.market.model.PriceLevel;
import com.worldtrader.api.market.model.Side;

import java.time.Instant;
import java.util.*;

public class OrderBook {
    private record OrderLocator(Side side, long priceTicks, Order ref) {}

    private final String ticker;
    private final NavigableMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Long, PriceLevel> asks = new TreeMap<>();
    private final Map<String, OrderLocator> orderIndex = new HashMap<>();

    public OrderBook(String ticker) { this.ticker = ticker; }
    public String ticker() { return ticker; }
    public NavigableMap<Long, PriceLevel> bids() { return bids; }
    public NavigableMap<Long, PriceLevel> asks() { return asks; }

    public Long bestBidTicks() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Long bestAskTicks() { return asks.isEmpty() ? null : asks.firstKey(); }

    public void add(Order o) {
        var sideMap = o.side() == Side.BUY ? bids : asks;
        var level = sideMap.computeIfAbsent(o.priceTicks(), PriceLevel::new);
        level.add(o);
        orderIndex.put(o.orderId(), new OrderLocator(o.side(), o.priceTicks(), o));
    }

    public boolean cancel(String orderId) {
        OrderLocator loc = orderIndex.get(orderId);
        if (loc != null) {
            NavigableMap<Long, PriceLevel> map = loc.side() == Side.BUY ? bids : asks;
            PriceLevel level = map.get(loc.priceTicks());
            if (level == null) {
                orderIndex.remove(orderId);
                return false;
            }
            boolean removed = level.queue().remove(loc.ref());
            if (removed) {
                level.reduce(loc.ref().remainingQty());
                if (level.empty()) map.remove(loc.priceTicks());
                orderIndex.remove(orderId);
                return true;
            }
            orderIndex.remove(orderId);
            return false;
        }
        return false;
    }

    public void removeIndex(String orderId) { orderIndex.remove(orderId); }

    public Order getOrder(String orderId) {
        OrderLocator locator = orderIndex.get(orderId);
        return locator == null ? null : locator.ref();
    }

    public List<Order> restingOrders() {
        List<Order> out = new ArrayList<>();
        bids.values().forEach(l -> out.addAll(l.queue()));
        asks.values().forEach(l -> out.addAll(l.queue()));
        return out;
    }

    public int depthQty(Side side, int levels) {
        NavigableMap<Long, PriceLevel> map = side == Side.BUY ? bids : asks;
        return map.values().stream().limit(levels).mapToInt(PriceLevel::totalQty).sum();
    }

    public int totalRestingOrders() {
        return orderIndex.size();
    }

    public OrderBookSnapshot snapshot(int depth, PriceTicks priceTicks, Instant ts) {
        return new OrderBookSnapshot(ticker, snapshotSide(bids, depth, priceTicks), snapshotSide(asks, depth, priceTicks), ts);
    }

    private List<OrderBookSnapshot.Level> snapshotSide(NavigableMap<Long, PriceLevel> side, int depth, PriceTicks priceTicks) {
        if (depth <= 0 || side.isEmpty()) return List.of();
        List<OrderBookSnapshot.Level> out = new ArrayList<>(Math.min(depth, side.size()));
        int count = 0;
        for (Map.Entry<Long, PriceLevel> e : side.entrySet()) {
            PriceLevel level = e.getValue();
            out.add(new OrderBookSnapshot.Level(priceTicks.toPrice(e.getKey()), level.totalQty(), level.queue().size()));
            count++;
            if (count >= depth) break;
        }
        return List.copyOf(out);
    }
}
