package com.worldtrader.api.market.controller;

import com.worldtrader.api.market.dto.CandleDto;
import com.worldtrader.api.market.service.MarketSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candles")
public class CandleController {
    private static final Logger log = LoggerFactory.getLogger(CandleController.class);
    private final MarketSimulationService market;

    public CandleController(MarketSimulationService market) {
        this.market = market;
    }

    @GetMapping
    public ResponseEntity<List<CandleDto>> get(
            @RequestParam String ticker,
            @RequestParam(defaultValue = "1s") String tf,
            @RequestParam(defaultValue = "500") int limit
    ) {
        List<CandleDto> candles = market.getCandles(ticker, tf, limit);
        log.info("GET /candles ticker={} tf={} limit={} -> {}", ticker, tf, limit, candles.size());
        return ResponseEntity.ok(candles);
    }
}
