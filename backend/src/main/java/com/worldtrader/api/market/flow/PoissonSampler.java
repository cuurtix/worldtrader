package com.worldtrader.api.market.flow;

import java.util.concurrent.ThreadLocalRandom;

public final class PoissonSampler {
    private PoissonSampler() {}

    public static int sample(double lambda) {
        if (lambda <= 0) return 0;
        if (lambda > 40) {
            double gaussian = ThreadLocalRandom.current().nextGaussian() * Math.sqrt(lambda) + lambda;
            return Math.max(0, (int) Math.round(gaussian));
        }
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= ThreadLocalRandom.current().nextDouble();
        } while (p > l);
        return k - 1;
    }
}
