package com.worldtrader.api.market.secure.controller;

import com.worldtrader.api.market.secure.dto.CreateOrderAcceptedResponse;
import com.worldtrader.api.market.secure.dto.CreateOrderRequest;
import com.worldtrader.api.market.secure.error.InvalidOrderError;
import com.worldtrader.api.market.secure.model.OrderModel;
import com.worldtrader.api.market.secure.queue.QueueProcessor;
import com.worldtrader.api.market.secure.service.OrderService;
import com.worldtrader.api.market.secure.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/orders")
public class OrderController {
    private final OrderService orderService;
    private final QueueProcessor queueProcessor;
    private final RateLimitService rateLimitService;

    public OrderController(OrderService orderService, QueueProcessor queueProcessor, RateLimitService rateLimitService) {
        this.orderService = orderService;
        this.queueProcessor = queueProcessor;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderAcceptedResponse> submit(@RequestHeader("X-User-Id") String jwtUserId,
                                                              @Valid @RequestBody CreateOrderRequest request,
                                                              HttpServletRequest httpRequest) {
        if (!jwtUserId.equals(request.userId())) throw new InvalidOrderError("Forbidden account access");
        rateLimitService.check(jwtUserId, httpRequest.getRemoteAddr());
        OrderModel order = orderService.createQueuedOrder(request);
        queueProcessor.enqueue(order.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CreateOrderAcceptedResponse(order.getId(), "QUEUED"));
    }
}
