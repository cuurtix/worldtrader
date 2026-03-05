package com.worldtrader.api.market.secure;

import com.worldtrader.api.market.secure.dto.CreateOrderRequest;
import com.worldtrader.api.market.secure.error.InsufficientFundsError;
import com.worldtrader.api.market.secure.error.InvalidOrderError;
import com.worldtrader.api.market.secure.model.*;
import com.worldtrader.api.market.secure.repo.BalanceAuditRepository;
import com.worldtrader.api.market.secure.repo.BalanceRepository;
import com.worldtrader.api.market.secure.repo.InstrumentRepository;
import com.worldtrader.api.market.secure.repo.OrderRepository;
import com.worldtrader.api.market.secure.service.OrderService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderRepository orderRepository;
    private BalanceRepository balanceRepository;
    private InstrumentRepository instrumentRepository;
    private BalanceAuditRepository balanceAuditRepository;
    private RedisOperations<String, String> redisOps;
    private ValueOperations<String, String> valueOps;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        balanceRepository = Mockito.mock(BalanceRepository.class);
        instrumentRepository = Mockito.mock(InstrumentRepository.class);
        balanceAuditRepository = Mockito.mock(BalanceAuditRepository.class);
        redisOps = Mockito.mock(RedisOperations.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redisOps.opsForValue()).thenReturn(valueOps);
        service = new OrderService(orderRepository, balanceRepository, instrumentRepository, balanceAuditRepository, redisOps, new SimpleMeterRegistry());
    }

    @Test
    void executesTotalFormulaExactly() {
        OrderModel order = new OrderModel();
        order.setUserId("u1");
        order.setSymbol("BTC-USD");
        order.setOrderType(SecureOrderType.LIMIT);
        order.setQuantity(2);
        order.setPrice(new BigDecimal("60000.12345678"));
        order.setFee(new BigDecimal("1.00000000"));

        BalanceModel bal = new BalanceModel();
        bal.setUserId("u1");
        bal.setAmount(new BigDecimal("200000.00000000"));

        InstrumentModel instrument = new InstrumentModel();
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(balanceRepository.findByUserIdForUpdate("u1")).thenReturn(Optional.of(bal));
        when(instrumentRepository.findById("BTC-USD")).thenReturn(Optional.of(instrument));
        when(orderRepository.save(any(OrderModel.class))).thenAnswer(a -> a.getArgument(0));

        // reflection-free workaround for immutable model fields
        try {
            var field = InstrumentModel.class.getDeclaredField("precisionScale");
            field.setAccessible(true);
            field.set(instrument, 8);
            var symbolField = InstrumentModel.class.getDeclaredField("symbol");
            symbolField.setAccessible(true);
            symbolField.set(instrument, "BTC-USD");
            var tradableField = InstrumentModel.class.getDeclaredField("tradable");
            tradableField.setAccessible(true);
            tradableField.set(instrument, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OrderModel result = service.executeQueuedOrder("o1");
        assertEquals(new BigDecimal("120001.24691356"), result.getTotal());
    }

    @Test
    void rejectsInsufficientFunds() {
        OrderModel order = new OrderModel();
        order.setUserId("u1");
        order.setSymbol("BTC-USD");
        order.setOrderType(SecureOrderType.LIMIT);
        order.setQuantity(1);
        order.setPrice(new BigDecimal("100.00000000"));
        order.setFee(new BigDecimal("1.00000000"));

        BalanceModel bal = new BalanceModel();
        bal.setUserId("u1");
        bal.setAmount(new BigDecimal("50.00000000"));

        InstrumentModel instrument = new InstrumentModel();
        try {
            var field = InstrumentModel.class.getDeclaredField("precisionScale");
            field.setAccessible(true);
            field.set(instrument, 8);
            var symbolField = InstrumentModel.class.getDeclaredField("symbol");
            symbolField.setAccessible(true);
            symbolField.set(instrument, "BTC-USD");
            var tradableField = InstrumentModel.class.getDeclaredField("tradable");
            tradableField.setAccessible(true);
            tradableField.set(instrument, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(orderRepository.findById("o2")).thenReturn(Optional.of(order));
        when(balanceRepository.findByUserIdForUpdate("u1")).thenReturn(Optional.of(bal));
        when(instrumentRepository.findById("BTC-USD")).thenReturn(Optional.of(instrument));

        assertThrows(InsufficientFundsError.class, () -> service.executeQueuedOrder("o2"));
    }

    @Test
    void validatesLimitPrice() {
        CreateOrderRequest request = new CreateOrderRequest("u1", "BTC-USD", SecureOrderType.LIMIT, 1, null, BigDecimal.ZERO);
        assertThrows(InvalidOrderError.class, () -> service.createQueuedOrder(request));
    }
}
