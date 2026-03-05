package com.worldtrader.api.market.secure.service;

import com.worldtrader.api.market.secure.dto.CreateOrderRequest;
import com.worldtrader.api.market.secure.error.InsufficientFundsError;
import com.worldtrader.api.market.secure.error.InvalidOrderError;
import com.worldtrader.api.market.secure.error.SystemError;
import com.worldtrader.api.market.secure.model.*;
import com.worldtrader.api.market.secure.repo.BalanceAuditRepository;
import com.worldtrader.api.market.secure.repo.BalanceRepository;
import com.worldtrader.api.market.secure.repo.InstrumentRepository;
import com.worldtrader.api.market.secure.repo.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final BalanceRepository balanceRepository;
    private final InstrumentRepository instrumentRepository;
    private final BalanceAuditRepository balanceAuditRepository;
    private final RedisOperations<String, String> redisOps;
    private final ValueOperations<String, String> valueOps;
    private final Counter processedCounter;
    private final Counter errorCounter;
    private final Timer latencyTimer;

    public OrderService(OrderRepository orderRepository, BalanceRepository balanceRepository, InstrumentRepository instrumentRepository,
                        BalanceAuditRepository balanceAuditRepository, RedisOperations<String, String> redisOps, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.balanceRepository = balanceRepository;
        this.instrumentRepository = instrumentRepository;
        this.balanceAuditRepository = balanceAuditRepository;
        this.redisOps = redisOps;
        this.valueOps = redisOps.opsForValue();
        this.processedCounter = meterRegistry.counter("worldtrader_orders_processed_total");
        this.errorCounter = meterRegistry.counter("worldtrader_orders_error_total");
        this.latencyTimer = meterRegistry.timer("worldtrader_order_latency");
    }

    public OrderModel createQueuedOrder(CreateOrderRequest req) {
        validate(req);
        OrderModel order = new OrderModel();
        order.setUserId(req.userId());
        order.setSymbol(req.symbol());
        order.setOrderType(req.orderType());
        order.setQuantity(req.quantity());
        order.setPrice(req.price());
        order.setFee(req.fee() == null ? BigDecimal.ZERO : req.fee());
        order.setStatus(OrderStatus.QUEUED);
        return orderRepository.save(order);
    }

    @Retryable(
            retryFor = {DeadlockLoserDataAccessException.class, CannotAcquireLockException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 5)
    )
    @Transactional
    public OrderModel executeQueuedOrder(String orderId) {
        return latencyTimer.record(() -> {
            OrderModel order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new InvalidOrderError("Order not found"));

            BalanceModel balance = balanceRepository.findByUserIdForUpdate(order.getUserId())
                    .orElseThrow(() -> new InvalidOrderError("Balance not found"));

            int scale = instrumentRepository.findById(order.getSymbol())
                    .filter(InstrumentModel::isTradable)
                    .map(InstrumentModel::getPrecisionScale)
                    .orElseThrow(() -> new InvalidOrderError("Unknown symbol"));

            BigDecimal price = order.getOrderType() == SecureOrderType.MARKET
                    ? getCachedMarketPrice(order.getSymbol(), scale)
                    : requirePrice(order.getPrice(), scale);
            BigDecimal fee = order.getFee().setScale(scale, RoundingMode.UNNECESSARY);
            BigDecimal total = price.multiply(BigDecimal.valueOf(order.getQuantity())).add(fee).setScale(scale, RoundingMode.UNNECESSARY);

            if (balance.getAmount().compareTo(total) < 0) {
                errorCounter.increment();
                throw new InsufficientFundsError("Insufficient funds");
            }

            balance.setAmount(balance.getAmount().subtract(total).setScale(scale, RoundingMode.UNNECESSARY));
            balanceRepository.save(balance);

            BalanceAuditModel audit = new BalanceAuditModel();
            audit.setUserId(order.getUserId());
            audit.setAmount(total.negate());
            audit.setReason("ORDER_EXECUTION:" + order.getId());
            balanceAuditRepository.save(audit);

            order.setPrice(price);
            order.setTotal(total);
            order.setStatus(OrderStatus.FILLED);
            OrderModel persisted = orderRepository.save(order);
            processedCounter.increment();
            return persisted;
        });
    }

    private BigDecimal getCachedMarketPrice(String symbol, int scale) {
        String key = "market:price:" + symbol;
        String cached = valueOps.get(key);
        if (cached == null) throw new InvalidOrderError("Market price unavailable");
        return new BigDecimal(cached).setScale(scale, RoundingMode.UNNECESSARY);
    }

    public void cacheInstrumentConfig(String symbol, String payload) {
        valueOps.set("instrument:config:" + symbol, payload, Duration.ofHours(1));
    }

    public void cacheUserProfile(String userId, String payload) {
        valueOps.set("user:profile:" + userId, payload, Duration.ofMinutes(5));
    }

    public void invalidateUserProfile(String userId) {
        redisOps.delete("user:profile:" + userId);
    }

    private BigDecimal requirePrice(BigDecimal price, int scale) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidOrderError("Price required for non-market order");
        return price.setScale(scale, RoundingMode.UNNECESSARY);
    }

    private void validate(CreateOrderRequest req) {
        if (req.quantity() == null || req.quantity() <= 0) throw new InvalidOrderError("Quantity must be > 0");
        if (req.orderType() == SecureOrderType.LIMIT && (req.price() == null || req.price().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new InvalidOrderError("Limit price must be positive");
        }
    }

    @Recover
    public OrderModel recover(RuntimeException ex, String orderId) {
        errorCounter.increment();
        throw new SystemError("Order execution failed after retries", ex);
    }
}
