package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchingEngine {

    public List<Trade> execute(OrderBook book, Order incoming) {
        List<Trade> trades = new ArrayList<>();
        if (incoming.side() == Side.BUY) {
            matchBuy(book, incoming, trades);
        } else {
            matchSell(book, incoming, trades);
        }
        if (incoming.remainingQty() > 0 && incoming.type() == OrderType.LIMIT && incoming.tif() == TimeInForce.GTC) {
            book.add(incoming);
        }
        return trades;
    }

    private void matchBuy(OrderBook book, Order incoming, List<Trade> trades) {
        while (incoming.remainingQty() > 0 && !book.asks().isEmpty()) {
            double askPx = book.asks().firstKey();
            if (incoming.type() == OrderType.LIMIT && incoming.limitPrice() < askPx) break;
            var level = book.asks().get(askPx);
            var maker = level.queue().peekFirst();
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), askPx, fill, Side.BUY, Instant.now(), incoming.orderId(), maker.orderId(), incoming.traderId(), maker.traderId()));
            if (maker.remainingQty() <= 0) level.queue().pollFirst();
            if (level.empty()) book.asks().pollFirstEntry();
        }
    }

    private void matchSell(OrderBook book, Order incoming, List<Trade> trades) {
        while (incoming.remainingQty() > 0 && !book.bids().isEmpty()) {
            double bidPx = book.bids().firstKey();
            if (incoming.type() == OrderType.LIMIT && incoming.limitPrice() > bidPx) break;
            var level = book.bids().get(bidPx);
            var maker = level.queue().peekFirst();
            int fill = Math.min(incoming.remainingQty(), maker.remainingQty());
            maker.decreaseRemaining(fill);
            incoming.decreaseRemaining(fill);
            level.reduce(fill);
            trades.add(new Trade(UUID.randomUUID().toString(), incoming.ticker(), bidPx, fill, Side.SELL, Instant.now(), maker.orderId(), incoming.orderId(), maker.traderId(), incoming.traderId()));
            if (maker.remainingQty() <= 0) level.queue().pollFirst();
            if (level.empty()) book.bids().pollFirstEntry();
        }
    }
}
