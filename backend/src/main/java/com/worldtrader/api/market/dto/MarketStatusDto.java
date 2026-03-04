package com.worldtrader.api.market.dto;

public record MarketStatusDto(boolean running, long intervalMillis, long tickCount) {}
