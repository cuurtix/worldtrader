package com.worldtrader.api.market.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Portfolio {
    private final String traderId;
    private double cash;
    private double realizedPnl;
    private final Map<String, Position> positions = new ConcurrentHashMap<>();

    public Portfolio(String traderId, double initialCash) {
        this.traderId = traderId;
        this.cash = initialCash;
    }

    public String traderId() { return traderId; }
    public double cash() { return cash; }
    public double realizedPnl() { return realizedPnl; }
    public Map<String, Position> positions() { return positions; }

    public void applyBuy(String ticker, int qty, double price) {
        cash -= qty * price;
        positions.computeIfAbsent(ticker, t -> new Position()).buy(qty, price);
    }

    public void applySell(String ticker, int qty, double price) {
        cash += qty * price;
        Position p = positions.computeIfAbsent(ticker, t -> new Position());
        realizedPnl += p.sell(qty, price);
    }
}
