package com.worldtrader.api.market.service;

import com.worldtrader.api.exception.StockNotFoundException;
import com.worldtrader.api.market.agent.AgentContext;
import com.worldtrader.api.market.agent.MarketMakerAgent;
import com.worldtrader.api.market.dto.*;
import com.worldtrader.api.market.engine.MatchingEngine;
import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.flow.*;
import com.worldtrader.api.market.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
    private final List<MarketMakerAgent> marketMakers = new ArrayList<>();
    private final List<OrderFlowCategory> flowCategories = new ArrayList<>();
    private final AtomicLong tickCount = new AtomicLong(0);

    private final Map<String, Set<String>> activeOrdersByTrader = new ConcurrentHashMap<>();
    private final Map<String, String> orderOwner = new ConcurrentHashMap<>();

    private final AtomicLong submittedOrders = new AtomicLong(0);
    private final AtomicLong cancelRequests = new AtomicLong(0);
    private final AtomicLong cancelSuccess = new AtomicLong(0);
    private final AtomicLong totalTrades = new AtomicLong(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private volatile long intervalMillis = 300;
    private volatile boolean running = true;

    private final MarketSimulationProperties properties;

    public MarketSimulationService(MarketSimulationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        bootstrap();
        task = scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void bootstrap() {
        if (catalog.isEmpty()) {
            seed();
            initMarketMakers(properties.getMarketMakers());
            flowCategories.add(new RetailNoiseFlowCategory());
            flowCategories.add(new MomentumFlowCategory());
            flowCategories.add(new MeanReversionFlowCategory());
            flowCategories.add(new NewsShockFlowCategory());
            flowCategories.add(new ArbFlowCategory());
            flowCategories.add(new LiquidationFlowCategory());
        }
    }

    @PreDestroy
    void stop() {
        if (task != null) task.cancel(true);
        scheduler.shutdownNow();
    }

    private void initMarketMakers(int n) {
        for (int i = 0; i < n; i++) {
            double baseSpread = 0.0008 + ThreadLocalRandom.current().nextDouble(0.0, 0.0012);
            double riskAversion = 0.6 + ThreadLocalRandom.current().nextDouble(0.0, 1.8);
            double invTarget = ThreadLocalRandom.current().nextDouble(-100.0, 100.0);
            double cancelRate = 2.0 + ThreadLocalRandom.current().nextDouble(0.0, 5.0);
            double refreshRate = 1.2 + ThreadLocalRandom.current().nextDouble(0.0, 3.0);
            int minSize = 10 + ThreadLocalRandom.current().nextInt(0, 40);
            int maxSize = minSize + 40 + ThreadLocalRandom.current().nextInt(0, 120);
            MarketMakerAgent.Params p = new MarketMakerAgent.Params(baseSpread, riskAversion, invTarget, cancelRate, refreshRate, properties.getMmLevels(), minSize, maxSize);
            marketMakers.add(new MarketMakerAgent("MM_" + i, p));
        }
    }

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
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.BUY, OrderType.LIMIT, TimeInForce.GTC, 300, px - 0.15, Instant.now()));
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 300, px + 0.15, Instant.now()));
    }

    public Set<String> tickers() { return catalog.keySet(); }
    public String companyName(String ticker) { return catalog.get(ticker); }

    public synchronized void tick() {
        bootstrap();
        if (!running) return;
        tickCount.incrementAndGet();
        double dt = intervalMillis / 1000.0;
        driftRegime(dt);

        int cancelBudget = regime.getMaxCancelsPerTick();
        cancelBudget -= runMarketMakerCancels(dt, cancelBudget);
        cancelBudget -= runFlowCancels(dt, cancelBudget);

        int orderBudget = regime.getMaxOrdersPerTick();
        orderBudget -= runMarketMakerQuotes(orderBudget);
        orderBudget -= runFlowCategories(dt, orderBudget);
    }

    private void driftRegime(double dt) {
        double slow = 0.01 * dt;
        regime.setRiskOnOff(regime.getRiskOnOff() + ThreadLocalRandom.current().nextDouble(-slow, slow));
        regime.setPoliticalStress(regime.getPoliticalStress() + ThreadLocalRandom.current().nextDouble(-slow, slow));
        regime.setLiquidity(regime.getLiquidity() + ThreadLocalRandom.current().nextDouble(-slow, slow));
        regime.setNewsIntensity(regime.getNewsIntensity() + ThreadLocalRandom.current().nextDouble(-slow * 1.2, slow * 1.2));
        regime.setBurstiness(regime.getBurstiness() + ThreadLocalRandom.current().nextDouble(-slow, slow));
    }

    private int runMarketMakerQuotes(int budget) {
        if (budget <= 0) return 0;
        int submitted = 0;
        AgentContext ctx = new AgentContext(regime, this);
        for (MarketMakerAgent mm : marketMakers) {
            double pRefresh = Math.min(1.0, mm.params().refreshRate() * (intervalMillis / 1000.0));
            if (ThreadLocalRandom.current().nextDouble() > pRefresh) continue;
            for (OrderRequest req : mm.generate(ctx)) {
                if (submitted >= budget) return submitted;
                try {
                    submitOrder(req);
                    submitted++;
                } catch (Exception ignored) {}
            }
        }
        return submitted;
    }

    private int runFlowCategories(double dt, int budget) {
        if (budget <= 0) return 0;
        int submitted = 0;
        for (String ticker : tickers()) {
            OrderFlowContext context = new OrderFlowContext(regime, this, ticker);
            for (OrderFlowCategory category : flowCategories) {
                double lambda = category.intensity(context);
                int count = PoissonSampler.sample(lambda * dt);
                if (count == 0) continue;
                for (OrderIntent intent : category.generateOrders(context, dt, count)) {
                    if (submitted >= budget) return submitted;
                    try {
                        submitOrder(category.buildOrder(intent));
                        submitted++;
                    } catch (Exception ignored) {}
                }
            }
        }
        return submitted;
    }

    private int runMarketMakerCancels(double dt, int budget) {
        if (budget <= 0) return 0;
        int canceled = 0;
        for (MarketMakerAgent mm : marketMakers) {
            int draws = PoissonSampler.sample(mm.params().cancelRate() * dt * (1.0 + regime.getPoliticalStress()));
            canceled += cancelFromActiveSet(mm.traderId(), draws, budget - canceled, true);
            if (canceled >= budget) return canceled;
        }
        return canceled;
    }

    private int runFlowCancels(double dt, int budget) {
        if (budget <= 0) return 0;
        int draws = PoissonSampler.sample((1.0 + 4.0 * regime.getBurstiness()) * dt);
        return cancelFromActiveSet("FLOW_", draws, budget, false);
    }

    private int cancelFromActiveSet(String traderPattern, int draws, int budget, boolean exactTrader) {
        if (draws <= 0 || budget <= 0) return 0;
        int canceled = 0;
        for (Map.Entry<String, Set<String>> entry : activeOrdersByTrader.entrySet()) {
            String trader = entry.getKey();
            if ((exactTrader && !trader.equals(traderPattern)) || (!exactTrader && !trader.startsWith(traderPattern))) {
                continue;
            }
            List<String> ids = new ArrayList<>(entry.getValue());
            Collections.shuffle(ids);
            for (String id : ids) {
                if (canceled >= budget || canceled >= draws) return canceled;
                cancelRequests.incrementAndGet();
                if (cancelOrder(id)) {
                    canceled++;
                    cancelSuccess.incrementAndGet();
                }
            }
        }
        return canceled;
    }

    public synchronized OrderResponse submitOrder(OrderRequest request) {
        validateOrder(request);
        String ticker = normalizeTicker(request.ticker());
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.traderId(),
                ticker,
                request.side(),
                request.type(),
                request.tif() == null ? TimeInForce.GTC : request.tif(),
                request.qty(),
                request.type() == OrderType.LIMIT ? request.price() : null,
                Instant.now()
        );
        return submitInternal(order);
    }

    private OrderResponse submitInternal(Order order) {
        submittedOrders.incrementAndGet();
        OrderBook book = books.get(order.ticker());
        List<Trade> fills = matching.execute(book, order);
        fills.forEach(this::recordTrade);
        for (Trade t : fills) applyPortfolio(t);

        if (order.type() == OrderType.LIMIT && order.tif() == TimeInForce.GTC && order.remainingQty() > 0) {
            orderOwner.put(order.orderId(), order.traderId());
            activeOrdersByTrader.computeIfAbsent(order.traderId(), k -> ConcurrentHashMap.newKeySet()).add(order.orderId());
        }

        OrderStatus status = fills.isEmpty() && order.type() == OrderType.MARKET
                ? OrderStatus.REJECTED
                : order.remainingQty() == 0
                ? OrderStatus.FILLED
                : fills.isEmpty() ? OrderStatus.NEW : OrderStatus.PARTIALLY_FILLED;
        return new OrderResponse(order.orderId(), status, fills.stream().map(f -> new FillDto(f.tradeId(), f.price(), f.qty())).toList(), order.remainingQty());
    }

    private void applyPortfolio(Trade t) {
        Portfolio buyer = portfolios.computeIfAbsent(t.buyerTraderId(), id -> new Portfolio(id, 1_000_000));
        Portfolio seller = portfolios.computeIfAbsent(t.sellerTraderId(), id -> new Portfolio(id, 1_000_000));
        buyer.applyBuy(t.ticker(), t.qty(), t.price());
        seller.applySell(t.ticker(), t.qty(), t.price());
    }

    public synchronized boolean cancelOrder(String orderId) {
        boolean canceled = books.values().stream().anyMatch(b -> b.cancel(orderId));
        if (canceled) {
            String owner = orderOwner.remove(orderId);
            if (owner != null) {
                Set<String> set = activeOrdersByTrader.get(owner);
                if (set != null) set.remove(orderId);
            }
        }
        return canceled;
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
        double buyAgg = tw.stream().filter(t -> t.aggressorSide() == Side.BUY).mapToDouble(Trade::qty).sum();
        double sellAgg = tw.stream().filter(t -> t.aggressorSide() == Side.SELL).mapToDouble(Trade::qty).sum();
        double ofi = buyAgg - sellAgg;
        double nofi = ofi / (buyAgg + sellAgg + 1e-9);
        double li = (db - da) / (double) (db + da + 1e-9);
        double rv = Math.sqrt(returnsWin.get(ticker).stream().mapToDouble(x -> x * x).sum());
        double vwapNum = tw.stream().mapToDouble(t -> t.price() * t.qty()).sum();
        double vwapDen = tw.stream().mapToDouble(Trade::qty).sum();
        double vwap = vwapDen == 0 ? getLastPrice(ticker) : vwapNum / vwapDen;
        return new MetricsDto(ticker, b.mid(), b.spread(), db, da, ofi, nofi, li, rv, vwap);
    }

    public MarketMicroStatsDto getMicroStats() {
        double avgSpread = tickers().stream().map(this::getMetrics).mapToDouble(m -> m.spread() == null ? 0.0 : m.spread()).average().orElse(0.0);
        double avgDepth = tickers().stream().map(this::getMetrics).mapToDouble(m -> (m.depthBid() + m.depthAsk()) / 2.0).average().orElse(0.0);
        double avgTradeSize = trades.values().stream().flatMap(Collection::stream).limit(2000).mapToDouble(Trade::qty).average().orElse(0.0);
        double acf = lag1ReturnAutocorr();
        long resting = books.values().stream().mapToLong(OrderBook::totalRestingOrders).sum();
        double ctr = totalTrades.get() == 0 ? 0.0 : (double) cancelSuccess.get() / totalTrades.get();
        return new MarketMicroStatsDto(tickCount.get(), submittedOrders.get(), cancelRequests.get(), cancelSuccess.get(), totalTrades.get(), ctr, avgSpread, avgDepth, avgTradeSize, acf, resting);
    }

    private double lag1ReturnAutocorr() {
        List<Double> rs = returnsWin.values().stream().flatMap(Collection::stream).limit(2000).toList();
        if (rs.size() < 3) return 0.0;
        double mean = rs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double num = 0.0;
        double den = 0.0;
        for (int i = 1; i < rs.size(); i++) {
            num += (rs.get(i) - mean) * (rs.get(i - 1) - mean);
        }
        for (double r : rs) den += (r - mean) * (r - mean);
        return den == 0 ? 0.0 : num / den;
    }

    public MarketRegime getRegime() { return regime; }

    public void updateRegime(MarketRegime input) {
        regime.setRiskOnOff(input.getRiskOnOff());
        regime.setCentralBank(input.getCentralBank());
        regime.setPoliticalStress(input.getPoliticalStress());
        regime.setLiquidity(input.getLiquidity());
        regime.setNewsIntensity(input.getNewsIntensity());
        regime.setVolatilityTarget(input.getVolatilityTarget());
        regime.setBurstiness(input.getBurstiness());
        regime.setMaxOrdersPerTick(input.getMaxOrdersPerTick());
        regime.setMaxCancelsPerTick(input.getMaxCancelsPerTick());
    }

    public double getLastPrice(String ticker) {
        List<Trade> ts = getTrades(ticker, 1);
        if (!ts.isEmpty()) return ts.get(0).price();
        OrderBook b = books.get(normalizeTicker(ticker));
        if (b.mid() != null) return b.mid();
        return b.bestBid() != null ? b.bestBid() : b.bestAsk() != null ? b.bestAsk() : 0.0;
    }

    private void recordTrade(Trade t) {
        totalTrades.incrementAndGet();
        Deque<Trade> dq = trades.get(t.ticker());
        dq.addFirst(t);
        while (dq.size() > 1500) dq.pollLast();

        Deque<Trade> tw = tradeWin.get(t.ticker());
        tw.addFirst(t);
        while (tw.size() > 300) tw.pollLast();

        Deque<Double> rw = returnsWin.get(t.ticker());
        if (dq.size() > 1) {
            double p0 = dq.stream().skip(1).findFirst().map(Trade::price).orElse(t.price());
            if (p0 > 0) rw.addFirst(Math.log(t.price() / p0));
        }
        while (rw.size() > 300) rw.pollLast();

        // cleanup non-resting owners from active map lazily (incoming can be fully filled)
        orderOwner.remove(t.buyOrderId());
        orderOwner.remove(t.sellOrderId());
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
        if (request.type() == OrderType.LIMIT && (request.price() == null || request.price() <= 0)) {
            throw new IllegalArgumentException("limit price must be > 0");
        }
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
