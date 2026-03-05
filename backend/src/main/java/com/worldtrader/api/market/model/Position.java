package com.worldtrader.api.market.model;

public class Position {
    private int qty;
    private double avgCost;

    public int qty() { return qty; }
    public double avgCost() { return avgCost; }

    public void buy(int fillQty, double fillPrice) {
        double totalCost = avgCost * qty + fillPrice * fillQty;
        qty += fillQty;
        avgCost = qty == 0 ? 0.0 : totalCost / qty;
    }

    public double sell(int fillQty, double fillPrice) {
        int executed = Math.min(fillQty, qty);
        double realized = (fillPrice - avgCost) * executed;
        qty -= executed;
        if (qty == 0) avgCost = 0.0;
        return realized;
    }
}
