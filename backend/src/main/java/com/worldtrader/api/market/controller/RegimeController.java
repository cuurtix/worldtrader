package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.model.MarketRegime;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/regime")
public class RegimeController {
    private final MarketSimulationService market;
    public RegimeController(MarketSimulationService market) { this.market = market; }

    @GetMapping
    public ResponseEntity<MarketRegime> get() { return ResponseEntity.ok(market.getRegime()); }

    @PutMapping
    public ResponseEntity<MarketRegime> put(@RequestBody MarketRegime regime) { market.updateRegime(regime); return ResponseEntity.ok(market.getRegime()); }
}
