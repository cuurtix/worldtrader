package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.MarketMicroStatsDto;
import com.worldtrader.api.market.dto.MetricsDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {
    private final MarketSimulationService market;
    public MetricsController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{ticker}")
    public ResponseEntity<MetricsDto> get(@PathVariable String ticker) { return ResponseEntity.ok(market.getMetrics(ticker)); }

    @GetMapping("/market")
    public ResponseEntity<MarketMicroStatsDto> marketStats() { return ResponseEntity.ok(market.getMicroStats()); }
}
