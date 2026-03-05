package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.Trade;

import java.util.List;

public final class MatchResult {
    private final List<Trade> fills;
    private final List<String> fullyFilledMakerOrderIds;
    private final long stpSkips;

    public MatchResult(List<Trade> fills, List<String> fullyFilledMakerOrderIds, long stpSkips) {
        this.fills = List.copyOf(fills);
        this.fullyFilledMakerOrderIds = List.copyOf(fullyFilledMakerOrderIds);
        this.stpSkips = stpSkips;
    }

    public List<Trade> getFills() {
        return fills;
    }

    public List<String> getFullyFilledMakerOrderIds() {
        return fullyFilledMakerOrderIds;
    }

    public long getStpSkips() {
        return stpSkips;
    }
}
