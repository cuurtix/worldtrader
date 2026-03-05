package com.worldtrader.api.market.engine;

import com.worldtrader.api.market.model.Trade;

import java.util.List;

public final class MatchResult {
    private final List<Trade> fills;
    private final List<String> fullyFilledMakerOrderIds;

    public MatchResult(List<Trade> fills, List<String> fullyFilledMakerOrderIds) {
        this.fills = List.copyOf(fills);
        this.fullyFilledMakerOrderIds = List.copyOf(fullyFilledMakerOrderIds);
    }

    public List<Trade> getFills() {
        return fills;
    }

    public List<String> getFullyFilledMakerOrderIds() {
        return fullyFilledMakerOrderIds;
    }
}
