package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.OrderRequest;
import com.worldtrader.api.market.dto.OrderResponse;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final MarketSimulationService market;
    public OrderController(MarketSimulationService market) { this.market = market; }

    @PostMapping
    public ResponseEntity<OrderResponse> submit(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(market.submitOrder(request));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancel(@PathVariable String orderId) {
        return market.cancelOrder(orderId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
