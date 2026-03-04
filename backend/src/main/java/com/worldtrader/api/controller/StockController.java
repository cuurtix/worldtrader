package com.worldtrader.api.controller;

import com.worldtrader.api.model.Stock;
import com.worldtrader.api.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @Operation(summary = "List all stocks")
    public ResponseEntity<List<Stock>> getStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }

    @GetMapping("/{ticker:.+}")
    @Operation(summary = "Get stock by ticker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock found"),
            @ApiResponse(responseCode = "404", description = "Ticker not found", content = @Content(schema = @Schema(implementation = com.worldtrader.api.exception.ApiError.class)))
    })
    public ResponseEntity<Stock> getIndividualStockData(
            @Parameter(description = "Ticker symbol, case-insensitive") @PathVariable String ticker,
            @RequestParam(value = "view", required = false, defaultValue = "BASIC") String view
    ) {
        return ResponseEntity.ok(stockService.getStockByTicker(ticker));
    }

    @GetMapping("/price/{ticker:.+}")
    @Operation(summary = "Get stock price by ticker", description = "Returns a numeric JSON value (double)")
    public ResponseEntity<Double> getStockPrice(@PathVariable String ticker) {
        Stock stock = stockService.getStockByTicker(ticker);
        return ResponseEntity.ok(stock.price());
    }

    @GetMapping("/prices")
    @Operation(summary = "Batch prices by comma-separated tickers")
    public ResponseEntity<Map<String, Double>> getPricesByTickers(@RequestParam("tickers") String tickers) {
        if (tickers == null || tickers.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'tickers' is required");
        }
        Map<String, Double> out = new LinkedHashMap<>();
        for (String token : tickers.split(",")) {
            String normalized = stockService.normalizeTicker(token);
            out.put(normalized, stockService.getPriceByTicker(normalized));
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/random")
    @Operation(summary = "Get a random stock")
    public ResponseEntity<Stock> randomStock() {
        return ResponseEntity.ok(stockService.getRandomStock());
    }
}
