package com.worldtrader.api.market.dto;

public record MetricsDto(String ticker, Double mid, Double spread, int depthBid, int depthAsk, double ofi, double nofi, double li, double rv, double vwap) {}
