package com.worldtrader.api.market.dto;

import java.util.List;

public record PortfolioDto(String traderId, double cash, double realizedPnl, double unrealizedPnl, List<PositionDto> positions) {}
