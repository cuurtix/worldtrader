package com.worldtrader.api.market.dto;

public record CandleDto(long t, double o, double h, double l, double c, double v) {
    public static class MutableCandle {
        public long t;
        public double o;
        public double h;
        public double l;
        public double c;
        public double v;

        public MutableCandle(long t, double o, double h, double l, double c, double v) {
            this.t = t;
            this.o = o;
            this.h = h;
            this.l = l;
            this.c = c;
            this.v = v;
        }
    }
}
