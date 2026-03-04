package com.worldtrader.api.market.agent;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.flow.OrderSizeModel;
import com.worldtrader.api.market.model.OrderType;
import com.worldtrader.api.market.model.Side;
import com.worldtrader.api.market.model.TimeInForce;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MarketMakerAgent implements MarketAgent {
    public record Params(
            double baseSpread,
            double riskAversion,
            double inventoryTarget,
            double cancelRate,
            double refreshRate,
            int levels,
            int minSize,
            int maxSize
    ) {}

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
            double liquidityPenalty = 1.0 / Math.max(0.08, regime.getLiquidity());
            double spread = mid * params.baseSpread() * (1 + 2.0 * rv + 1.7 * regime.getPoliticalStress()) * liquidityPenalty;

            double invQty = portfolio.positions().stream().filter(p -> p.ticker().equals(t)).findFirst().map(p -> (double) p.qty()).orElse(0.0);
            double skew = params.riskAversion() * Math.tanh((invQty - params.inventoryTarget()) / 180.0) * mid * 0.0008;

            double pullProb = Math.min(0.85, 0.05 + 0.6 * regime.getPoliticalStress() + 0.5 * (1.0 - regime.getLiquidity()));
            if (ThreadLocalRandom.current().nextDouble() < pullProb) {
                continue;
            }

            for (int lvl = 0; lvl < params.levels(); lvl++) {
                double levelFactor = 1.0 + 0.35 * lvl;
                double levelOffset = spread * levelFactor;

                int bidSize = sampleQuoteSize(regime);
                int askSize = sampleQuoteSize(regime);

                out.add(new OrderRequest(traderId, t, Side.BUY, OrderType.LIMIT, bidSize, Math.max(0.01, mid - levelOffset - skew), TimeInForce.GTC));
                out.add(new OrderRequest(traderId, t, Side.SELL, OrderType.LIMIT, askSize, Math.max(0.01, mid + levelOffset - skew), TimeInForce.GTC));
            }
        }
        return out;
    }

    private int sampleQuoteSize(com.worldtrader.api.market.model.MarketRegime regime) {
        int raw = Math.max(params.minSize(), Math.min(params.maxSize(), OrderSizeModel.sampleHeavyTailSize()));
        double liqScale = Math.max(0.2, regime.getLiquidity());
        return Math.max(1, (int) Math.round(raw * liqScale));
    }
}
