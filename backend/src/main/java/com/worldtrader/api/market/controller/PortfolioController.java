package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.PortfolioDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {
    private final MarketSimulationService market;
    public PortfolioController(MarketSimulationService market) { this.market = market; }

    @GetMapping("/{traderId}")
    public ResponseEntity<PortfolioDto> get(@PathVariable String traderId) { return ResponseEntity.ok(market.getPortfolio(traderId)); }
}
