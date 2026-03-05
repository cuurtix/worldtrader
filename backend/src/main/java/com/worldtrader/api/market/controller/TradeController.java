package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.TradeTickDto;
import com.worldtrader.api.market.model.Trade;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    private static final Logger log = LoggerFactory.getLogger(TradeController.class);
    private final MarketSimulationService market;

    public TradeController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{ticker}")
    public ResponseEntity<List<Trade>> get(@PathVariable String ticker, @RequestParam(defaultValue = "200") int limit) {
        List<Trade> out = market.getTrades(ticker, limit);
        log.info("GET /trades/{} limit={} -> {}", ticker, limit, out.size());
        return ResponseEntity.ok(out);
    }

    @GetMapping
    public ResponseEntity<List<TradeTickDto>> getByTicker(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "200") int limit
    ) {
        List<TradeTickDto> out = market.getTradesView(ticker, limit);
        log.info("GET /trades ticker={} limit={} -> {}", ticker, limit, out.size());
        return ResponseEntity.ok(out);
    }
}
