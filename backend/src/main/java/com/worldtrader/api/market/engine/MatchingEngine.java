package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchingEngine {
    private final PriceTicks priceTicks;

    public MatchingEngine(PriceTicks priceTicks) {
        this.priceTicks = priceTicks;
    }

    public MatchResult execute(OrderBook book, Order incoming, Instant ts) {
        List<Trade> trades = new ArrayList<>();
        List<String> filledMakers = new ArrayList<>();
        if (incoming.side() == Side.BUY) {
            matchBuy(book, incoming, trades, filledMakers, ts);
        } else {
            matchSell(book, incoming, trades, filledMakers, ts);
        }
        if (incoming.remainingQty() > 0 && incoming.type() == OrderType.LIMIT && incoming.tif() == TimeInForce.GTC) {
            book.add(incoming);
        }
        return new MatchResult(trades, filledMakers);
    }

    private void matchBuy(OrderBook book, Order incoming, List<Trade> trades, List<String> filledMakers, Instant ts) {
        while (incoming.remainingQty() > 0 && !book.asks().isEmpty()) {
            long askPxTicks = book.asks().firstKey();
            if (incoming.type() == OrderType.LIMIT && incoming.priceTicks() < askPxTicks) break;
            var level = book.asks().get(askPxTicks);
            var maker = level.queue().peekFirst();
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), priceTicks.toPrice(askPxTicks), fill, Side.BUY, ts, incoming.orderId(), maker.orderId(), incoming.traderId(), maker.traderId()));
            if (maker.remainingQty() <= 0) {
                level.queue().pollFirst();
                book.removeIndex(maker.orderId());
                filledMakers.add(maker.orderId());
            }
            if (level.empty()) book.asks().pollFirstEntry();
        }
    }

    private void matchSell(OrderBook book, Order incoming, List<Trade> trades, List<String> filledMakers, Instant ts) {
        while (incoming.remainingQty() > 0 && !book.bids().isEmpty()) {
            long bidPxTicks = book.bids().firstKey();
            if (incoming.type() == OrderType.LIMIT && incoming.priceTicks() > bidPxTicks) break;
            var level = book.bids().get(bidPxTicks);
            var maker = level.queue().peekFirst();
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), priceTicks.toPrice(bidPxTicks), fill, Side.SELL, ts, maker.orderId(), incoming.orderId(), maker.traderId(), incoming.traderId()));
            if (maker.remainingQty() <= 0) {
                level.queue().pollFirst();
                book.removeIndex(maker.orderId());
                filledMakers.add(maker.orderId());
            }
            if (level.empty()) book.bids().pollFirstEntry();
        }
    }
}
