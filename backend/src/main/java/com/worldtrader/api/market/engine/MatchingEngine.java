package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchingEngine {
    private final PriceTicks priceTicks;

    public MatchingEngine(PriceTicks priceTicks) {
        this.priceTicks = priceTicks;
    }

    public MatchResult execute(OrderBook book, Order incoming, Instant ts) {
        List<Trade> trades = new ArrayList<>();
        List<String> filledMakers = new ArrayList<>();
        long stpSkips = 0;
        if (incoming.side() == Side.BUY) {
            stpSkips = matchBuy(book, incoming, trades, filledMakers, ts);
        } else {
            stpSkips = matchSell(book, incoming, trades, filledMakers, ts);
        }
        if (incoming.remainingQty() > 0 && incoming.type() == OrderType.LIMIT && incoming.tif() == TimeInForce.GTC) {
            book.add(incoming);
        }
        return new MatchResult(trades, filledMakers, stpSkips);
    }

    public boolean canFullyFill(OrderBook book, Order incoming) {
        int needed = incoming.remainingQty();
        if (incoming.side() == Side.BUY) {
            for (Map.Entry<Long, PriceLevel> e : book.asks().entrySet()) {
                long px = e.getKey();
                if (incoming.type() == OrderType.LIMIT && px > incoming.priceTicks()) break;
                for (Order maker : e.getValue().queue()) {
                    if (!maker.traderId().equals(incoming.traderId())) needed -= maker.remainingQty();
                    if (needed <= 0) return true;
                }
            }
        } else {
            for (Map.Entry<Long, PriceLevel> e : book.bids().entrySet()) {
                long px = e.getKey();
                if (incoming.type() == OrderType.LIMIT && px < incoming.priceTicks()) break;
                for (Order maker : e.getValue().queue()) {
                    if (!maker.traderId().equals(incoming.traderId())) needed -= maker.remainingQty();
                    if (needed <= 0) return true;
                }
            }
        }
        return false;
    }

    private long matchBuy(OrderBook book, Order incoming, List<Trade> trades, List<String> filledMakers, Instant ts) {
        long stpSkips = 0;
        while (incoming.remainingQty() > 0 && !book.asks().isEmpty()) {
            Map.Entry<Long, PriceLevel> levelEntry = firstMatchableLevelForBuy(book, incoming);
            if (levelEntry == null) break;
            long askPxTicks = levelEntry.getKey();
            var level = levelEntry.getValue();
            Order maker = firstNonSelf(level, incoming.traderId());
            if (maker == null) {
                stpSkips += level.queue().size();
                break;
            }
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), priceTicks.toPrice(askPxTicks), fill, Side.BUY, ts, incoming.orderId(), maker.orderId(), incoming.traderId(), maker.traderId()));
            if (maker.remainingQty() <= 0) {
                level.queue().remove(maker);
                book.removeIndex(maker.orderId());
                filledMakers.add(maker.orderId());
            }
            if (level.empty()) book.asks().pollFirstEntry();
        }
        return stpSkips;
    }

    private long matchSell(OrderBook book, Order incoming, List<Trade> trades, List<String> filledMakers, Instant ts) {
        long stpSkips = 0;
        while (incoming.remainingQty() > 0 && !book.bids().isEmpty()) {
            Map.Entry<Long, PriceLevel> levelEntry = firstMatchableLevelForSell(book, incoming);
            if (levelEntry == null) break;
            long bidPxTicks = levelEntry.getKey();
            var level = levelEntry.getValue();
            Order maker = firstNonSelf(level, incoming.traderId());
            if (maker == null) {
                stpSkips += level.queue().size();
                break;
            }
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), priceTicks.toPrice(bidPxTicks), fill, Side.SELL, ts, maker.orderId(), incoming.orderId(), maker.traderId(), incoming.traderId()));
            if (maker.remainingQty() <= 0) {
                level.queue().remove(maker);
                book.removeIndex(maker.orderId());
                filledMakers.add(maker.orderId());
            }
            if (level.empty()) book.bids().pollFirstEntry();
        }
        return stpSkips;
    }

    private Order firstNonSelf(PriceLevel level, String traderId) {
        for (Order maker : level.queue()) {
            if (!maker.traderId().equals(traderId)) return maker;
        }
        return null;
    }

    private Map.Entry<Long, PriceLevel> firstMatchableLevelForBuy(OrderBook book, Order incoming) {
        for (Map.Entry<Long, PriceLevel> e : book.asks().entrySet()) {
            if (incoming.type() == OrderType.LIMIT && e.getKey() > incoming.priceTicks()) return null;
            if (firstNonSelf(e.getValue(), incoming.traderId()) != null) return e;
        }
        return null;
    }

    private Map.Entry<Long, PriceLevel> firstMatchableLevelForSell(OrderBook book, Order incoming) {
        for (Map.Entry<Long, PriceLevel> e : book.bids().entrySet()) {
            if (incoming.type() == OrderType.LIMIT && e.getKey() < incoming.priceTicks()) return null;
            if (firstNonSelf(e.getValue(), incoming.traderId()) != null) return e;
        }
        return null;
    }
}
