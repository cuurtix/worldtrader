package com.worldtrader.api.market.service;

import com.worldtrader.api.exception.StockNotFoundException;
import com.worldtrader.api.market.agent.*;
import com.worldtrader.api.market.dto.*;
import com.worldtrader.api.market.engine.MatchingEngine;
import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MarketSimulationService {
    private final Map<String, String> catalog = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<String, Deque<Trade>> trades = new ConcurrentHashMap<>();
    private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final Map<String, Deque<Double>> returnsWin = new ConcurrentHashMap<>();
    private final Map<String, Deque<Trade>> tradeWin = new ConcurrentHashMap<>();
    private final MarketRegime regime = new MarketRegime();
    private final MatchingEngine matching = new MatchingEngine();
    private final List<MarketAgent> agents = new ArrayList<>();
    private final AtomicLong tickCount = new AtomicLong(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private volatile long intervalMillis = 300;
    private volatile boolean running = true;

    @PostConstruct
    void init() {
        seed();
        agents.add(new MarketMakerAgent());
        agents.add(new NoiseTraderAgent());
        agents.add(new MomentumAgent());
        agents.add(new MeanReversionAgent());
        task = scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() { if (task != null) task.cancel(true); scheduler.shutdownNow(); }

    private void seed() {
        addTicker("AAPL", "Apple Inc.", 190.0);
        addTicker("MSFT", "Microsoft", 420.0);
        addTicker("TSLA", "Tesla", 180.0);
    }

    private void addTicker(String t, String n, double px) {
        catalog.put(t, n);
        OrderBook b = new OrderBook(t);
        books.put(t, b);
        trades.put(t, new ConcurrentLinkedDeque<>());
        returnsWin.put(t, new ConcurrentLinkedDeque<>());
        tradeWin.put(t, new ConcurrentLinkedDeque<>());
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.BUY, OrderType.LIMIT, TimeInForce.GTC, 200, px - 0.1, Instant.now()));
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 200, px + 0.1, Instant.now()));
    }

    public Set<String> tickers() { return catalog.keySet(); }
    public String companyName(String ticker){ return catalog.get(ticker); }

    public synchronized void tick() {
        if (!running) return;
        tickCount.incrementAndGet();
        driftRegime();
        AgentContext ctx = new AgentContext(regime, this);
        for (var a : agents) {
            for (OrderRequest req : a.generate(ctx)) {
                try { submitOrder(req); } catch (Exception ignored) {}
            }
        }
    }

    private void driftRegime() {
        double d = ThreadLocalRandom.current().nextDouble(-0.03, 0.03);
        regime.setRiskOnOff(regime.getRiskOnOff() + d);
        regime.setPoliticalStress(regime.getPoliticalStress() + ThreadLocalRandom.current().nextDouble(-0.02, 0.02));
    }

    public synchronized OrderResponse submitOrder(OrderRequest request) {
        validateOrder(request);
        String ticker = normalizeTicker(request.ticker());
        Order order = new Order(UUID.randomUUID().toString(), request.traderId(), ticker, request.side(), request.type(), request.tif()==null?TimeInForce.GTC:request.tif(), request.qty(), request.type()==OrderType.LIMIT?request.price():null, Instant.now());
        return submitInternal(order);
    }

    private OrderResponse submitInternal(Order order) {
        OrderBook book = books.get(order.ticker());
        List<Trade> fills = matching.execute(book, order);
        fills.forEach(this::recordTrade);
        for (Trade t : fills) applyPortfolio(t);
        OrderStatus status = fills.isEmpty() && order.type()==OrderType.MARKET ? OrderStatus.REJECTED : order.remainingQty()==0 ? OrderStatus.FILLED : fills.isEmpty()?OrderStatus.NEW:OrderStatus.PARTIALLY_FILLED;
        return new OrderResponse(order.orderId(), status, fills.stream().map(f->new FillDto(f.tradeId(), f.price(), f.qty())).toList(), order.remainingQty());
    }

    private void applyPortfolio(Trade t) {
        Portfolio buyer = portfolios.computeIfAbsent(t.buyerTraderId(), id->new Portfolio(id, 1_000_000));
        Portfolio seller = portfolios.computeIfAbsent(t.sellerTraderId(), id->new Portfolio(id, 1_000_000));
        buyer.applyBuy(t.ticker(), t.qty(), t.price());
        seller.applySell(t.ticker(), t.qty(), t.price());
    }

    public synchronized boolean cancelOrder(String orderId) {
        return books.values().stream().anyMatch(b -> b.cancel(orderId));
    }

    public OrderBookDto getOrderBook(String rawTicker, int levels) {
        String ticker = normalizeTicker(rawTicker);
        OrderBook b = books.get(ticker);
        if (b == null) throw new StockNotFoundException(ticker);
        List<LevelDto> bidLevels = b.bids().values().stream().limit(levels).map(l -> new LevelDto(l.price(), l.totalQty())).toList();
        List<LevelDto> askLevels = b.asks().values().stream().limit(levels).map(l -> new LevelDto(l.price(), l.totalQty())).toList();
        int bidDepth = bidLevels.stream().mapToInt(LevelDto::qty).sum();
        int askDepth = askLevels.stream().mapToInt(LevelDto::qty).sum();
        double imbalance = (bidDepth - askDepth) / (double) (bidDepth + askDepth + 1e-9);
        return new OrderBookDto(ticker, b.bestBid(), b.bestAsk(), b.spread(), bidLevels, askLevels, imbalance);
    }

    public List<Trade> getTrades(String rawTicker, int limit) {
        String ticker = normalizeTicker(rawTicker);
        Deque<Trade> dq = trades.get(ticker);
        if (dq == null) throw new StockNotFoundException(ticker);
        return dq.stream().limit(limit).toList();
    }

    public PortfolioDto getPortfolio(String traderId) {
        Portfolio p = portfolios.computeIfAbsent(traderId, id -> new Portfolio(id, 1_000_000));
        double unrealized = p.positions().entrySet().stream().mapToDouble(e -> {
            double m = getLastPrice(e.getKey());
            return (m - e.getValue().avgCost()) * e.getValue().qty();
        }).sum();
        var positions = p.positions().entrySet().stream().map(e -> new PositionDto(e.getKey(), e.getValue().qty(), e.getValue().avgCost())).toList();
        return new PortfolioDto(traderId, p.cash(), p.realizedPnl(), unrealized, positions);
    }

    public MetricsDto getMetrics(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        OrderBook b = books.get(ticker);
        if (b == null) throw new StockNotFoundException(ticker);
        int db = b.depthQty(Side.BUY, 10), da = b.depthQty(Side.SELL, 10);
        Deque<Trade> tw = tradeWin.get(ticker);
        double buyAgg = tw.stream().filter(t -> t.aggressorSide()==Side.BUY).mapToDouble(Trade::qty).sum();
        double sellAgg = tw.stream().filter(t -> t.aggressorSide()==Side.SELL).mapToDouble(Trade::qty).sum();
        double ofi = buyAgg - sellAgg;
        double nofi = ofi / (buyAgg + sellAgg + 1e-9);
        double li = (db - da) / (double) (db + da + 1e-9);
        double rv = Math.sqrt(returnsWin.get(ticker).stream().mapToDouble(x -> x * x).sum());
        double vwapNum = tw.stream().mapToDouble(t -> t.price() * t.qty()).sum();
        double vwapDen = tw.stream().mapToDouble(Trade::qty).sum();
        double vwap = vwapDen == 0 ? getLastPrice(ticker) : vwapNum / vwapDen;
        return new MetricsDto(ticker, b.mid(), b.spread(), db, da, ofi, nofi, li, rv, vwap);
    }

    public MarketRegime getRegime() { return regime; }
    public void updateRegime(MarketRegime input) {
        regime.setRiskOnOff(input.getRiskOnOff());
        regime.setCentralBank(input.getCentralBank());
        regime.setPoliticalStress(input.getPoliticalStress());
        regime.setLiquidity(input.getLiquidity());
        regime.setNewsIntensity(input.getNewsIntensity());
        regime.setVolatilityTarget(input.getVolatilityTarget());
    }

    public double getLastPrice(String ticker) {
        List<Trade> ts = getTrades(ticker, 1);
        if (!ts.isEmpty()) return ts.get(0).price();
        OrderBook b = books.get(normalizeTicker(ticker));
        if (b.mid()!=null) return b.mid();
        return b.bestBid()!=null?b.bestBid():b.bestAsk()!=null?b.bestAsk():0.0;
    }

    private void recordTrade(Trade t) {
        Deque<Trade> dq = trades.get(t.ticker());
        dq.addFirst(t);
        while (dq.size() > 1000) dq.pollLast();
        Deque<Trade> tw = tradeWin.get(t.ticker());
        tw.addFirst(t);
        while (tw.size() > 200) tw.pollLast();
        Deque<Double> rw = returnsWin.get(t.ticker());
        if (dq.size() > 1) {
            double p0 = dq.stream().skip(1).findFirst().map(Trade::price).orElse(t.price());
            if (p0 > 0) rw.addFirst(Math.log(t.price() / p0));
        }
        while (rw.size() > 200) rw.pollLast();
    }

    private String normalizeTicker(String t) {
        if (t == null || t.trim().isEmpty()) throw new IllegalArgumentException("ticker is required");
        String n = t.trim().toUpperCase(Locale.ROOT);
        if (!catalog.containsKey(n)) throw new StockNotFoundException(n);
        return n;
    }

    private void validateOrder(OrderRequest request) {
        if (request == null) throw new IllegalArgumentException("order body required");
        if (request.traderId() == null || request.traderId().isBlank()) throw new IllegalArgumentException("traderId required");
        if (request.qty() <= 0) throw new IllegalArgumentException("qty must be > 0");
        if (request.type() == OrderType.LIMIT && (request.price() == null || request.price() <= 0)) throw new IllegalArgumentException("limit price must be > 0");
    }

    public synchronized void setIntervalMillis(long millis) {
        if (millis < 20) throw new IllegalArgumentException("millis must be >= 20");
        intervalMillis = millis;
        if (task != null) task.cancel(false);
        task = scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void pause() { running = false; }
    public void resume() { running = true; }
    public MarketStatusDto status() { return new MarketStatusDto(running, intervalMillis, tickCount.get()); }
}
