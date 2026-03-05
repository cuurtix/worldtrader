package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.OrderBookDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orderbook")
public class OrderBookController {
    private final MarketSimulationService market;
    public OrderBookController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{ticker}")
    public ResponseEntity<OrderBookDto> get(@PathVariable String ticker, @RequestParam(defaultValue = "20") int levels) {
        return ResponseEntity.ok(market.getOrderBook(ticker, levels));
    }
}
