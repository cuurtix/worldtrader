package com.worldtrader.api.market.flow;

import java.util.concurrent.ThreadLocalRandom;

public final class OrderSizeModel {
    private OrderSizeModel() {}

    public static int sampleHeavyTailSize() {
        boolean block = ThreadLocalRandom.current().nextDouble() < 0.05;
        double mu = block ? 3.5 : 1.1;
        double sigma = block ? 0.9 : 0.5;
        double z = ThreadLocalRandom.current().nextGaussian();
        int q = (int) Math.round(Math.exp(mu + sigma * z));
        return Math.max(1, Math.min(q, 2000));
    }
}
