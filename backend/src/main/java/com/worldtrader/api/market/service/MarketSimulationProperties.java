package com.worldtrader.api.market.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market.sim")
public class MarketSimulationProperties {
    private int marketMakers = 12;
    private int mmLevels = 5;

    public int getMarketMakers() { return marketMakers; }
    public void setMarketMakers(int marketMakers) { this.marketMakers = Math.max(5, Math.min(30, marketMakers)); }

    public int getMmLevels() { return mmLevels; }
    public void setMmLevels(int mmLevels) { this.mmLevels = Math.max(3, Math.min(8, mmLevels)); }
}
