package com.worldtrader.api.market.model;

public class MarketRegime {
    private double riskOnOff = 0.1;
    private double centralBank = 0.1;
    private double politicalStress = 0.2;
    private double liquidity = 0.8;
    private double newsIntensity = 0.2;
    private double volatilityTarget = 0.2;
    private double burstiness = 0.25;
    private int maxOrdersPerTick = 500;
    private int maxCancelsPerTick = 250;

    public double getRiskOnOff() { return riskOnOff; }
    public void setRiskOnOff(double v) { this.riskOnOff = clamp(v, -1, 1); }
    public double getCentralBank() { return centralBank; }
    public void setCentralBank(double v) { this.centralBank = clamp(v, -1, 1); }
    public double getPoliticalStress() { return politicalStress; }
    public void setPoliticalStress(double v) { this.politicalStress = clamp(v, 0, 1); }
    public double getLiquidity() { return liquidity; }
    public void setLiquidity(double v) { this.liquidity = clamp(v, 0, 1); }
    public double getNewsIntensity() { return newsIntensity; }
    public void setNewsIntensity(double v) { this.newsIntensity = clamp(v, 0, 1); }
    public double getVolatilityTarget() { return volatilityTarget; }
    public void setVolatilityTarget(double v) { this.volatilityTarget = clamp(v, 0, 1); }
    public double getBurstiness() { return burstiness; }
    public void setBurstiness(double v) { this.burstiness = clamp(v, 0, 1); }
    public int getMaxOrdersPerTick() { return maxOrdersPerTick; }
    public void setMaxOrdersPerTick(int v) { this.maxOrdersPerTick = Math.max(10, Math.min(v, 5000)); }
    public int getMaxCancelsPerTick() { return maxCancelsPerTick; }
    public void setMaxCancelsPerTick(int v) { this.maxCancelsPerTick = Math.max(0, Math.min(v, 5000)); }

    private double clamp(double x, double lo, double hi) { return Math.max(lo, Math.min(hi, x)); }
}
