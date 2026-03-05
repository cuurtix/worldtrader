package com.worldtrader.api.market.secure.queue;

import com.worldtrader.api.market.secure.model.OrderModel;
import com.worldtrader.api.market.secure.model.OrderStatus;
import com.worldtrader.api.market.secure.repo.OrderRepository;
import com.worldtrader.api.market.secure.service.OrderService;
import com.worldtrader.api.market.secure.ws.OrderWebSocketNotifier;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class QueueProcessor {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> deadLetterQueue = new LinkedBlockingQueue<>();
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderWebSocketNotifier notifier;

    public QueueProcessor(OrderService orderService, OrderRepository orderRepository, OrderWebSocketNotifier notifier, MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notifier = notifier;
        Gauge.builder("worldtrader_order_queue_size", queue::size).register(meterRegistry);
        Gauge.builder("worldtrader_dead_letter_queue_size", deadLetterQueue::size).register(meterRegistry);
    }

    public void enqueue(String orderId) {
        queue.offer(orderId);
        processAsync();
    }

    @Async
    public void processAsync() {
        String orderId = queue.poll();
        if (orderId == null) return;
        int[] delays = {1000, 5000, 25000};
        for (int i = 0; i < delays.length; i++) {
            try {
                OrderModel order = orderService.executeQueuedOrder(orderId);
                notifier.notifyOrderUpdate(order);
                return;
            } catch (Exception ex) {
                if (i == delays.length - 1) {
                    deadLetterQueue.offer(orderId);
                    orderRepository.findById(orderId).ifPresent(o -> {
                        o.setStatus(OrderStatus.FAILED);
                        orderRepository.save(o);
                    });
                } else {
                    try { Thread.sleep(delays[i]); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }
}
