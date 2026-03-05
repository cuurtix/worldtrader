package com.worldtrader.api.market.dto;

public record PositionDto(String ticker, int qty, double avgCost) {}
