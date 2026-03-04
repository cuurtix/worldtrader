package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MarketMakerAgent implements MarketAgent {
    public record Params(double baseSpread, double riskAversion, double inventoryTarget, double cancelRate, double refreshRate, int minSize, int maxSize) {}

    private final String traderId;
    private final Params params;

    public MarketMakerAgent(String traderId, Params params) {
        this.traderId = traderId;
        this.params = params;
    }

    public String traderId() { return traderId; }
    public Params params() { return params; }

    @Override
    public List<OrderRequest> generate(AgentContext context) {
        List<OrderRequest> out = new ArrayList<>();
        var regime = context.regime();
        var portfolio = context.market().getPortfolio(traderId);

        for (String t : context.market().tickers()) {
            double mid = context.market().getLastPrice(t);
            var metrics = context.market().getMetrics(t);
            double rv = metrics.rv();
            double spread = mid * params.baseSpread() * (1 + 1.8 * rv + 1.5 * regime.getPoliticalStress()) / Math.max(0.05, regime.getLiquidity());
            double invQty = portfolio.positions().stream().filter(p -> p.ticker().equals(t)).findFirst().map(p -> (double) p.qty()).orElse(0.0);
            double skew = params.riskAversion() * Math.tanh((invQty - params.inventoryTarget()) / 200.0) * mid * 0.0005;
            int bidSize = ThreadLocalRandom.current().nextInt(params.minSize(), params.maxSize() + 1);
            int askSize = ThreadLocalRandom.current().nextInt(params.minSize(), params.maxSize() + 1);
            out.add(new OrderRequest(traderId, t, Side.BUY, OrderType.LIMIT, bidSize, Math.max(0.01, mid - spread / 2.0 - skew), TimeInForce.GTC));
            out.add(new OrderRequest(traderId, t, Side.SELL, OrderType.LIMIT, askSize, Math.max(0.01, mid + spread / 2.0 - skew), TimeInForce.GTC));
        }
        return out;
    }
}
