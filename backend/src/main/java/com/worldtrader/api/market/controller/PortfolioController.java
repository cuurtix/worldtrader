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
    public ResponseEntity<PortfolioDto> get(@PathVariable String traderId) {
        return ResponseEntity.ok(market.getPortfolio(traderId));
    }

    @PostMapping("/{traderId}/deposit")
    public ResponseEntity<PortfolioDto> deposit(@PathVariable String traderId, @RequestParam double amount) {
        return ResponseEntity.ok(market.deposit(traderId, amount));
    }

    @PostMapping("/{traderId}/withdraw")
    public ResponseEntity<PortfolioDto> withdraw(@PathVariable String traderId, @RequestParam double amount) {
        return ResponseEntity.ok(market.withdraw(traderId, amount));
    }
}
