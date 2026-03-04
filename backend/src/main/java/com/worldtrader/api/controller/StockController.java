package com.worldtrader.api.controller;

import com.worldtrader.api.model.Stock;
import com.worldtrader.api.model.StockPriceResponse;
import com.worldtrader.api.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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

    @GetMapping("/{ticker}")
    @Operation(summary = "Get stock by ticker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock found"),
            @ApiResponse(responseCode = "404", description = "Ticker not found", content = @Content(schema = @Schema(implementation = com.worldtrader.api.exception.ApiError.class)))
    })
    public ResponseEntity<Stock> getStockByTicker(
            @Parameter(description = "Ticker symbol, case-insensitive") @PathVariable String ticker) {
        return ResponseEntity.ok(stockService.getStockByTicker(ticker));
    }

    @GetMapping("/price/{ticker:.+}")
    @Operation(summary = "Get price by ticker")
    public ResponseEntity<StockPriceResponse> getPriceByTicker(@PathVariable String ticker) {
        String normalized = stockService.normalizeTicker(ticker);
        double price = stockService.getPriceByTicker(normalized);
        return ResponseEntity.ok(new StockPriceResponse(normalized, price, Instant.now()));
    }

    @GetMapping("/prices")
    @Operation(summary = "Batch prices by comma-separated tickers")
    public ResponseEntity<Map<String, Double>> getPricesByTickers(@RequestParam("tickers") String tickers) {
        return ResponseEntity.ok(stockService.getPricesByTickers(tickers));
    }

    @GetMapping("/random")
    @Operation(summary = "Get a random stock")
    public ResponseEntity<Stock> randomStock() {
        return ResponseEntity.ok(stockService.getRandomStock());
    }
}
