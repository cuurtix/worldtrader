package com.worldtrader.api.market.dto;

import java.util.List;

public record OrderBookDto(String ticker, Double bestBid, Double bestAsk, Double spread, List<LevelDto> bids, List<LevelDto> asks, double imbalance) {}
