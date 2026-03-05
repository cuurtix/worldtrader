package com.worldtrader.api.market.dto;

import java.time.Instant;

public record MarketStatusDto(boolean running, long intervalMillis, long tickCount, Instant date) {}
