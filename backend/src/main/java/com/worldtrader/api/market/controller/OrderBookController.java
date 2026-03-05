package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.OrderBookDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orderbook")
public class OrderBookController {
    private static final Logger log = LoggerFactory.getLogger(OrderBookController.class);
    private final MarketSimulationService market;
    public OrderBookController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{ticker}")
    public ResponseEntity<OrderBookDto> get(@PathVariable String ticker, @RequestParam(defaultValue = "20") int levels) {
        OrderBookDto dto = market.getOrderBook(ticker, levels);
        log.info("GET /orderbook/{} levels={} -> bids={} asks={}", ticker, levels, dto.bids().size(), dto.asks().size());
        return ResponseEntity.ok(dto);
    }
}
