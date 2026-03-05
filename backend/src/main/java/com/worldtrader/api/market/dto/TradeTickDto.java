package com.worldtrader.api.market.dto;

public record TradeTickDto(long t, double price, int qty, String side) {}
