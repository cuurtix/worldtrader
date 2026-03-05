package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.MarketStatusDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    private final MarketSimulationService market;
    public MarketController(MarketSimulationService market) { this.market = market; }

    @GetMapping
    public ResponseEntity<MarketStatusDto> status() { return ResponseEntity.ok(market.status()); }

    @PostMapping("/pause")
    public ResponseEntity<MarketStatusDto> pause() { market.pause(); return ResponseEntity.ok(market.status()); }

    @PostMapping("/resume")
    public ResponseEntity<MarketStatusDto> resume() { market.resume(); return ResponseEntity.ok(market.status()); }

    @PostMapping("/interval")
    public ResponseEntity<MarketStatusDto> interval(@RequestParam long millis) { market.setIntervalMillis(millis); return ResponseEntity.ok(market.status()); }
}
