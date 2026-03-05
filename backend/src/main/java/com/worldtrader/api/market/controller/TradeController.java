package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.model.Trade;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final MarketSimulationService market;
    public TradeController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{ticker}")
    public ResponseEntity<List<Trade>> get(@PathVariable String ticker, @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(market.getTrades(ticker, limit));
    }
}
