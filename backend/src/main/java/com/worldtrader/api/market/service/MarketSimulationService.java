package com.worldtrader.api.market.service;

import com.worldtrader.api.exception.StockNotFoundException;
import com.worldtrader.api.market.agent.AgentContext;
import com.worldtrader.api.market.agent.MarketMakerAgent;
import com.worldtrader.api.market.dto.*;
import com.worldtrader.api.market.engine.MatchResult;
import com.worldtrader.api.market.engine.MatchingEngine;
import com.worldtrader.api.market.engine.OrderBook;
import com.worldtrader.api.market.engine.PriceTicks;
import com.worldtrader.api.market.flow.*;
import com.worldtrader.api.market.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class MarketSimulationService {
    private static final Logger log = LoggerFactory.getLogger(MarketSimulationService.class);
    private record OrderMeta(String traderId, String ticker, Side side) {}
    private record OrderLocator(String ticker, Side side, long priceTicks) {}

    private final Map<String, String> catalog = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<String, Deque<Trade>> trades = new ConcurrentHashMap<>();
    private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final Map<String, Deque<Double>> returnsWin = new ConcurrentHashMap<>();
    private final Map<String, Deque<Trade>> tradeWin = new ConcurrentHashMap<>();
    private final MarketRegime regime = new MarketRegime();
    private final PriceTicks priceTicks = new PriceTicks(0.01);
    private final MatchingEngine matching = new MatchingEngine(priceTicks);
    private final List<MarketMakerAgent> marketMakers = new ArrayList<>();
    private final List<OrderFlowCategory> flowCategories = new ArrayList<>();
    private final AtomicLong tickCount = new AtomicLong(0);

    private final Map<String, Set<String>> activeOrdersByTrader = new ConcurrentHashMap<>();
    private final Map<String, OrderMeta> orderMeta = new ConcurrentHashMap<>();
    private final Map<String, OrderLocator> orderLocator = new ConcurrentHashMap<>();

    private final AtomicLong submittedOrders = new AtomicLong(0);
    private final AtomicLong cancelRequests = new AtomicLong(0);
    private final AtomicLong cancelSuccess = new AtomicLong(0);
    private final AtomicLong totalTrades = new AtomicLong(0);
    private final AtomicLong ordersAccepted = new AtomicLong(0);
    private final AtomicLong ordersRejected = new AtomicLong(0);
    private final AtomicLong cancelsOk = new AtomicLong(0);
    private final AtomicLong cancelsMiss = new AtomicLong(0);
    private final AtomicLong stpSkips = new AtomicLong(0);
    private final AtomicLong fokRejects = new AtomicLong(0);
    private final AtomicLong iocRemainderCanceled = new AtomicLong(0);
    private final AtomicLong submitErrors = new AtomicLong(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private volatile long intervalMillis = 300;
    private volatile boolean running = true;
    private Instant simulationTime;
    private final boolean noShortSelling = true;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private final MarketSimulationProperties properties;

    public MarketSimulationService(MarketSimulationProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        bootstrap();
        task = scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void bootstrap() {
        writeLock.lock();
        try {
            if (catalog.isEmpty()) {
                simulationTime = Instant.parse("2025-01-20T15:30:00Z");
                seed();
                initMarketMakers(properties.getMarketMakers());
                flowCategories.add(new RetailNoiseFlowCategory());
                flowCategories.add(new MomentumFlowCategory());
                flowCategories.add(new MeanReversionFlowCategory());
                flowCategories.add(new NewsShockFlowCategory());
                flowCategories.add(new ArbFlowCategory());
                flowCategories.add(new LiquidationFlowCategory());
            }
        } finally {
            writeLock.unlock();
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
        Portfolio seed = portfolios.computeIfAbsent("SEED_MM", id -> new Portfolio(id, 10_000_000));
        if (seed.position(t).qty() < 10_000) {
            seed.applyBuy(t, 10_000, px);
        }
        trades.put(t, new ConcurrentLinkedDeque<>());
        returnsWin.put(t, new ConcurrentLinkedDeque<>());
        tradeWin.put(t, new ConcurrentLinkedDeque<>());
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.BUY, OrderType.LIMIT, TimeInForce.GTC, 300, priceTicks.toTicks(px - 0.15), simulationTime));
        submitInternal(new Order(UUID.randomUUID().toString(), "SEED_MM", t, Side.SELL, OrderType.LIMIT, TimeInForce.GTC, 300, priceTicks.toTicks(px + 0.15), simulationTime));
    }

    public Set<String> tickers() {
        readLock.lock();
        try {
            return Set.copyOf(catalog.keySet());
        } finally {
            readLock.unlock();
        }
    }

    public String companyName(String ticker) {
        readLock.lock();
        try {
            return catalog.get(ticker);
        } finally {
            readLock.unlock();
        }
    }

    public void tick() {
        writeLock.lock();
        try {
        bootstrap();
        if (!running) return;
        simulationTime = simulationTime.plusMillis(intervalMillis);
        long tick = tickCount.incrementAndGet();
        double dt = intervalMillis / 1000.0;
        driftRegime(dt);

        int cancelBudget = regime.getMaxCancelsPerTick();
        cancelBudget -= runMarketMakerCancels(dt, cancelBudget);
        cancelBudget -= runFlowCancels(dt, cancelBudget);

        int orderBudget = regime.getMaxOrdersPerTick();
        orderBudget -= runMarketMakerQuotes(orderBudget);
        orderBudget -= runFlowCategories(dt, orderBudget);

        if (tick % 10 == 0) {
            long totalRecent = trades.values().stream().mapToLong(Deque::size).sum();
            log.info("sim tick={} running={} intervalMs={} totalRecentTrades={} submitted={} cancels={} trades={} accepted={} rejected={} stpSkips={} fokRejects={}", tick, running, intervalMillis, totalRecent, submittedOrders.get(), cancelSuccess.get(), totalTrades.get(), ordersAccepted.get(), ordersRejected.get(), stpSkips.get(), fokRejects.get());
        }
        } finally {
            writeLock.unlock();
        }
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
            enforceMaxActive(mm.traderId(), properties.getMaxActiveOrdersPerMmPerTicker());
            double pRefresh = Math.min(1.0, mm.params().refreshRate() * (intervalMillis / 1000.0));
            if (ThreadLocalRandom.current().nextDouble() > pRefresh) continue;
            for (OrderRequest req : mm.generate(ctx)) {
                if (submitted >= budget) return submitted;
                try {
                    submitOrder(req);
                    submitted++;
                } catch (Exception ex) {
                    submitErrors.incrementAndGet();
                    log.debug("market maker submit failed trader={} ticker={} reason={}", mm.traderId(), req.ticker(), ex.toString());
                }
            }
        }
        return submitted;
    }

    private void enforceMaxActive(String mmTraderId, int limitPerTicker) {
        Set<String> ids = activeOrdersByTrader.getOrDefault(mmTraderId, Collections.emptySet());
        Map<String, List<String>> byTicker = ids.stream()
                .filter(orderMeta::containsKey)
                .collect(Collectors.groupingBy(id -> orderMeta.get(id).ticker()));
        for (Map.Entry<String, List<String>> e : byTicker.entrySet()) {
            List<String> orderIds = new ArrayList<>(e.getValue());
            if (orderIds.size() <= limitPerTicker) continue;
            int extra = orderIds.size() - limitPerTicker;
            Collections.shuffle(orderIds);
            for (int i = 0; i < extra; i++) {
                cancelRequests.incrementAndGet();
                if (cancelOrder(orderIds.get(i))) cancelSuccess.incrementAndGet();
            }
        }
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
                    } catch (Exception ex) {
                        submitErrors.incrementAndGet();
                        log.debug("flow submit failed ticker={} category={} reason={}", ticker, category.getClass().getSimpleName(), ex.toString());
                    }
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
            if ((exactTrader && !trader.equals(traderPattern)) || (!exactTrader && !trader.startsWith(traderPattern))) continue;
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

    public OrderResponse submitOrder(OrderRequest request) {
        writeLock.lock();
        try {
            validateOrder(request);
            String ticker = normalizeTicker(request.ticker());
            TimeInForce tif = request.tif() == null ? TimeInForce.GTC : request.tif();
            long orderPriceTicks = request.type() == OrderType.LIMIT ? priceTicks.toTicks(request.price()) : 0L;
            Order order = new Order(UUID.randomUUID().toString(), request.traderId(), ticker, request.side(), request.type(), tif, request.qty(), orderPriceTicks, simulationTime);
            return submitInternal(order);
        } finally {
            writeLock.unlock();
        }
    }

    private OrderResponse submitInternal(Order order) {
        submittedOrders.incrementAndGet();
        OrderBook book = books.get(order.ticker());
        OrderRejectReason risk = validateRisk(order, book);
        if (risk != OrderRejectReason.NONE) {
            ordersRejected.incrementAndGet();
            return new OrderResponse(order.orderId(), OrderStatus.REJECTED, List.of(), order.remainingQty(), 0, 0, risk);
        }
        if (order.tif() == TimeInForce.FOK && !matching.canFullyFill(book, order)) {
            ordersRejected.incrementAndGet();
            fokRejects.incrementAndGet();
            return new OrderResponse(order.orderId(), OrderStatus.EXPIRED, List.of(), order.remainingQty(), 0, 0, OrderRejectReason.FOK_NOT_FILLED);
        }

        MatchResult result = matching.execute(book, order, simulationTime);
        stpSkips.addAndGet(result.getStpSkips());
        List<Trade> fills = result.getFills();
        fills.forEach(this::recordTrade);
        for (Trade t : fills) applyPortfolio(t);

        for (String makerOrderId : result.getFullyFilledMakerOrderIds()) {
            OrderMeta meta = orderMeta.remove(makerOrderId);
            if (meta != null) {
                Set<String> set = activeOrdersByTrader.get(meta.traderId());
                if (set != null) set.remove(makerOrderId);
            }
            orderLocator.remove(makerOrderId);
        }

        if (order.type() == OrderType.LIMIT && order.tif() == TimeInForce.GTC && order.remainingQty() > 0) {
            orderMeta.put(order.orderId(), new OrderMeta(order.traderId(), order.ticker(), order.side()));
            activeOrdersByTrader.computeIfAbsent(order.traderId(), k -> ConcurrentHashMap.newKeySet()).add(order.orderId());
            orderLocator.put(order.orderId(), new OrderLocator(order.ticker(), order.side(), order.priceTicks()));
        }

        int filledQty = fills.stream().mapToInt(Trade::qty).sum();
        int remainingCanceledQty = 0;
        OrderStatus status;
        OrderRejectReason reason = OrderRejectReason.NONE;
        if (fills.isEmpty() && order.type() == OrderType.MARKET) {
            status = OrderStatus.REJECTED;
            reason = OrderRejectReason.INSUFFICIENT_LIQUIDITY;
            ordersRejected.incrementAndGet();
        } else if (order.tif() == TimeInForce.IOC && order.remainingQty() > 0) {
            remainingCanceledQty = order.remainingQty();
            iocRemainderCanceled.addAndGet(remainingCanceledQty);
            status = fills.isEmpty() ? OrderStatus.CANCELED : OrderStatus.PARTIALLY_FILLED;
            ordersAccepted.incrementAndGet();
        } else {
            status = order.remainingQty() == 0 ? OrderStatus.FILLED : fills.isEmpty() ? OrderStatus.NEW : OrderStatus.PARTIALLY_FILLED;
            ordersAccepted.incrementAndGet();
        }
        return new OrderResponse(order.orderId(), status, fills.stream().map(f -> new FillDto(f.tradeId(), f.price(), f.qty())).toList(), order.remainingQty(), filledQty, remainingCanceledQty, reason);
    }

    private OrderRejectReason validateRisk(Order order, OrderBook book) {
        Portfolio p = portfolios.computeIfAbsent(order.traderId(), id -> new Portfolio(id, 1_000_000));
        if (order.side() == Side.BUY) {
            double estimated;
            if (order.type() == OrderType.LIMIT) {
                estimated = priceTicks.toPrice(order.priceTicks()) * order.quantity();
            } else {
                estimated = estimateMarketBuyNotional(book, order);
                if (estimated < 0) return OrderRejectReason.INSUFFICIENT_LIQUIDITY;
            }
            if (p.cash() < estimated) return OrderRejectReason.INSUFFICIENT_CASH;
        } else if (noShortSelling) {
            int positionQty = p.position(order.ticker()).qty();
            if (positionQty < order.quantity()) return OrderRejectReason.INSUFFICIENT_POSITION;
        }
        return OrderRejectReason.NONE;
    }

    private double estimateMarketBuyNotional(OrderBook book, Order order) {
        int needed = order.quantity();
        double notional = 0.0;
        for (Map.Entry<Long, PriceLevel> e : book.asks().entrySet()) {
            double px = priceTicks.toPrice(e.getKey());
            for (Order maker : e.getValue().queue()) {
                if (maker.traderId().equals(order.traderId())) continue;
                int qty = Math.min(needed, maker.remainingQty());
                notional += qty * px;
                needed -= qty;
                if (needed == 0) return notional;
            }
        }
        return -1.0;
    }

    private void applyPortfolio(Trade t) {
        Portfolio buyer = portfolios.computeIfAbsent(t.buyerTraderId(), id -> new Portfolio(id, 1_000_000));
        Portfolio seller = portfolios.computeIfAbsent(t.sellerTraderId(), id -> new Portfolio(id, 1_000_000));
        buyer.applyBuy(t.ticker(), t.qty(), t.price());
        seller.applySell(t.ticker(), t.qty(), t.price());
    }

    public boolean cancelOrder(String orderId) {
        writeLock.lock();
        try {
        OrderLocator locator = orderLocator.get(orderId);
        if (locator == null) {
            cancelsMiss.incrementAndGet();
            return false;
        }
        OrderBook book = books.get(locator.ticker());
        if (book == null) {
            orderLocator.remove(orderId);
            cancelsMiss.incrementAndGet();
            return false;
        }
        boolean canceled = book.cancel(orderId);
        if (canceled) {
            OrderMeta meta = orderMeta.remove(orderId);
            if (meta != null) {
                Set<String> set = activeOrdersByTrader.get(meta.traderId());
                if (set != null) set.remove(orderId);
            }
            orderLocator.remove(orderId);
            cancelsOk.incrementAndGet();
        } else {
            cancelsMiss.incrementAndGet();
            orderLocator.remove(orderId);
        }
        return canceled;
        } finally {
            writeLock.unlock();
        }
    }

    public int countActiveOrdersForTrader(String traderId) {
        readLock.lock();
        try {
        return activeOrdersByTrader.getOrDefault(traderId, Collections.emptySet()).size();
        } finally {
            readLock.unlock();
        }
    }

    public int getMarketMakerCount() {
        readLock.lock();
        try {
        return marketMakers.size();
        } finally {
            readLock.unlock();
        }
    }

    public OrderBookDto getOrderBook(String rawTicker, int levels) {
        readLock.lock();
        try {
        String ticker = normalizeTicker(rawTicker);
        OrderBook b = books.get(ticker);
        if (b == null) throw new StockNotFoundException(ticker);
        OrderBookSnapshot snapshot = b.snapshot(levels, priceTicks, simulationTime);
        List<LevelDto> bidLevels = snapshot.bids().stream().map(l -> new LevelDto(l.price(), l.totalQty())).toList();
        List<LevelDto> askLevels = snapshot.asks().stream().map(l -> new LevelDto(l.price(), l.totalQty())).toList();
        int bidDepth = bidLevels.stream().mapToInt(LevelDto::qty).sum();
        int askDepth = askLevels.stream().mapToInt(LevelDto::qty).sum();
        double imbalance = (bidDepth - askDepth) / (double) (bidDepth + askDepth + 1e-9);
        Double bestBid = bidLevels.isEmpty() ? null : bidLevels.get(0).price();
        Double bestAsk = askLevels.isEmpty() ? null : askLevels.get(0).price();
        Double spread = (bestBid == null || bestAsk == null) ? null : bestAsk - bestBid;
        return new OrderBookDto(ticker, bestBid, bestAsk, spread, bidLevels, askLevels, imbalance);
        } finally {
            readLock.unlock();
        }
    }

    public List<Trade> getTrades(String rawTicker, int limit) {
        readLock.lock();
        try {
        String ticker = normalizeTicker(rawTicker);
        Deque<Trade> dq = trades.get(ticker);
        if (dq == null) throw new StockNotFoundException(ticker);
        return List.copyOf(dq.stream().limit(limit).toList());
        } finally {
            readLock.unlock();
        }
    }



    public List<TradeTickDto> getTradesView(String rawTicker, int limit) {
        String ticker = normalizeTicker(rawTicker);
        List<TradeTickDto> out = getTrades(ticker, limit).stream()
                .map(t -> new TradeTickDto(t.timestamp().getEpochSecond(), t.price(), t.qty(), t.aggressorSide().name()))
                .toList();
        log.info("trades view ticker={} limit={} count={} sample={}", ticker, limit, out.size(), out.stream().limit(2).toList());
        return out;
    }

    public List<CandleDto> getCandles(String rawTicker, String tf, int limit) {
        readLock.lock();
        try {
        String ticker = normalizeTicker(rawTicker);
        int tfSec = parseTimeframe(tf);
        Map<Long, CandleDto.MutableCandle> buckets = new TreeMap<>();
        List<Trade> source = getTrades(ticker, Math.max(limit * 10, 500));
        source.stream()
                .sorted(Comparator.comparing(Trade::timestamp))
                .forEach(t -> {
                    long epoch = t.timestamp().getEpochSecond();
                    long bucket = (epoch / tfSec) * tfSec;
                    CandleDto.MutableCandle c = buckets.computeIfAbsent(bucket, b -> new CandleDto.MutableCandle(bucket, t.price(), t.price(), t.price(), t.price(), t.qty()));
                    c.h = Math.max(c.h, t.price());
                    c.l = Math.min(c.l, t.price());
                    c.c = t.price();
                    c.v += t.qty();
                });

        List<CandleDto> candles = buckets.values().stream()
                .map(c -> new CandleDto(c.t, c.o, c.h, c.l, c.c, c.v))
                .toList();

        if (candles.size() > limit) {
            candles = candles.subList(candles.size() - limit, candles.size());
        }
        CandleDto last = candles.isEmpty() ? null : candles.get(candles.size() - 1);
        log.info("candles ticker={} tf={}({}s) count={} last={}", ticker, tf, tfSec, candles.size(), last);
        return candles;
        } finally {
            readLock.unlock();
        }
    }

    private int parseTimeframe(String tf) {
        if (tf == null || tf.isBlank()) return 1;
        return switch (tf.trim().toLowerCase(Locale.ROOT)) {
            case "1s" -> 1;
            case "5s" -> 5;
            case "1m" -> 60;
            default -> throw new IllegalArgumentException("unsupported timeframe: " + tf);
        };
    }

    public PortfolioDto deposit(String traderId, double amount) {
        writeLock.lock();
        try {
        Portfolio p = portfolios.computeIfAbsent(traderId, id -> new Portfolio(id, 1_000_000));
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        p.adjustCash(amount);
        return getPortfolio(traderId);
        } finally {
            writeLock.unlock();
        }
    }

    public PortfolioDto withdraw(String traderId, double amount) {
        writeLock.lock();
        try {
        Portfolio p = portfolios.computeIfAbsent(traderId, id -> new Portfolio(id, 1_000_000));
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        p.adjustCash(-amount);
        return getPortfolio(traderId);
        } finally {
            writeLock.unlock();
        }
    }
    public PortfolioDto getPortfolio(String traderId) {
        readLock.lock();
        try {
        Portfolio p = portfolios.computeIfAbsent(traderId, id -> new Portfolio(id, 1_000_000));
        double unrealized = p.positions().entrySet().stream().mapToDouble(e -> (getLastPrice(e.getKey()) - e.getValue().avgCost()) * e.getValue().qty()).sum();
        var positions = p.positions().entrySet().stream().map(e -> new PositionDto(e.getKey(), e.getValue().qty(), e.getValue().avgCost())).toList();
        return new PortfolioDto(traderId, p.cash(), p.realizedPnl(), unrealized, positions);
        } finally {
            readLock.unlock();
        }
    }

    public MetricsDto getMetrics(String rawTicker) {
        readLock.lock();
        try {
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
        Double bestBid = b.bestBidTicks() == null ? null : priceTicks.toPrice(b.bestBidTicks());
        Double bestAsk = b.bestAskTicks() == null ? null : priceTicks.toPrice(b.bestAskTicks());
        Double mid = (bestBid == null || bestAsk == null) ? null : (bestBid + bestAsk) / 2.0;
        Double spread = (bestBid == null || bestAsk == null) ? null : bestAsk - bestBid;
        return new MetricsDto(ticker, mid, spread, db, da, ofi, nofi, li, rv, vwap);
        } finally {
            readLock.unlock();
        }
    }

    public MarketMicroStatsDto getMicroStats() {
        readLock.lock();
        try {
        List<MetricsDto> metrics = tickers().stream().map(this::getMetrics).toList();
        double spreadMean = metrics.stream().mapToDouble(m -> m.spread() == null ? 0.0 : m.spread()).average().orElse(0.0);
        double spreadP95 = percentile(metrics.stream().map(m -> m.spread() == null ? 0.0 : m.spread()).toList(), 0.95);
        double avgDepth = metrics.stream().mapToDouble(m -> (m.depthBid() + m.depthAsk()) / 2.0).average().orElse(0.0);

        List<Trade> recentTrades = trades.values().stream().flatMap(Collection::stream).limit(3000).toList();
        double tradeSizeMean = recentTrades.stream().mapToDouble(Trade::qty).average().orElse(0.0);
        double tradeSizeP95 = percentile(recentTrades.stream().map(t -> (double) t.qty()).toList(), 0.95);

        double acf = lag1ReturnAutocorr();
        double ofiRetCorr = ofiReturnCorr();
        double volCluster = volatilityClustering();

        long resting = books.values().stream().mapToLong(OrderBook::totalRestingOrders).sum();
        double cts = submittedOrders.get() == 0 ? 0.0 : (double) cancelSuccess.get() / submittedOrders.get();
        double ctt = totalTrades.get() == 0 ? 0.0 : (double) cancelSuccess.get() / totalTrades.get();

        return new MarketMicroStatsDto(tickCount.get(), submittedOrders.get(), cancelRequests.get(), cancelSuccess.get(), totalTrades.get(), cts, ctt, spreadMean, spreadP95, avgDepth, tradeSizeMean, tradeSizeP95, acf, ofiRetCorr, volCluster, resting, ordersAccepted.get(), ordersRejected.get(), cancelsOk.get(), cancelsMiss.get(), stpSkips.get(), fokRejects.get(), iocRemainderCanceled.get());
        } finally {
            readLock.unlock();
        }
    }

    private double percentile(List<Double> xs, double p) {
        if (xs.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(xs);
        sorted.sort(Double::compareTo);
        int idx = Math.min(sorted.size() - 1, Math.max(0, (int) Math.floor(p * (sorted.size() - 1))));
        return sorted.get(idx);
    }

    private double lag1ReturnAutocorr() {
        List<Double> rs = returnsWin.values().stream().flatMap(Collection::stream).limit(3000).toList();
        if (rs.size() < 3) return 0.0;
        double mean = rs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double num = 0.0, den = 0.0;
        for (int i = 1; i < rs.size(); i++) num += (rs.get(i) - mean) * (rs.get(i - 1) - mean);
        for (double r : rs) den += (r - mean) * (r - mean);
        return den == 0 ? 0.0 : num / den;
    }

    private double ofiReturnCorr() {
        List<Double> ofs = new ArrayList<>();
        List<Double> rets = new ArrayList<>();
        for (String t : tickers()) {
            MetricsDto m = getMetrics(t);
            ofs.add(m.nofi());
            rets.add(returnsWin.get(t).stream().findFirst().orElse(0.0));
        }
        return corr(ofs, rets);
    }

    private double volatilityClustering() {
        List<Double> absRet = returnsWin.values().stream().flatMap(Collection::stream).limit(3000).map(Math::abs).toList();
        if (absRet.size() < 3) return 0.0;
        return corr(absRet.subList(1, absRet.size()), absRet.subList(0, absRet.size() - 1));
    }

    private double corr(List<Double> a, List<Double> b) {
        int n = Math.min(a.size(), b.size());
        if (n < 2) return 0.0;
        double ma = a.stream().limit(n).mapToDouble(Double::doubleValue).average().orElse(0.0);
        double mb = b.stream().limit(n).mapToDouble(Double::doubleValue).average().orElse(0.0);
        double num = 0, da = 0, db = 0;
        for (int i = 0; i < n; i++) {
            double xa = a.get(i) - ma;
            double xb = b.get(i) - mb;
            num += xa * xb;
            da += xa * xa;
            db += xb * xb;
        }
        return da == 0 || db == 0 ? 0.0 : num / Math.sqrt(da * db);
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
        readLock.lock();
        try {
        List<Trade> ts = getTrades(ticker, 1);
        if (!ts.isEmpty()) return ts.get(0).price();
        OrderBook b = books.get(normalizeTicker(ticker));
        Double bestBid = b.bestBidTicks() == null ? null : priceTicks.toPrice(b.bestBidTicks());
        Double bestAsk = b.bestAskTicks() == null ? null : priceTicks.toPrice(b.bestAskTicks());
        if (bestBid != null && bestAsk != null) return (bestBid + bestAsk) / 2.0;
        return bestBid != null ? bestBid : bestAsk != null ? bestAsk : 0.0;
        } finally {
            readLock.unlock();
        }
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

    }

    public Instant simulationTime() {
        readLock.lock();
        try {
        return simulationTime;
        } finally {
            readLock.unlock();
        }
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

    public void setIntervalMillis(long millis) {
        writeLock.lock();
        try {
        if (millis < 20) throw new IllegalArgumentException("millis must be >= 20");
        intervalMillis = millis;
        if (task != null) task.cancel(false);
        task = scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        } finally {
            writeLock.unlock();
        }
    }

    public void pause() { writeLock.lock(); try { running = false; } finally { writeLock.unlock(); } }
    public void resume() { writeLock.lock(); try { running = true; } finally { writeLock.unlock(); } }
    public MarketStatusDto status() { readLock.lock(); try { return new MarketStatusDto(running, intervalMillis, tickCount.get(), simulationTime); } finally { readLock.unlock(); } }
}
