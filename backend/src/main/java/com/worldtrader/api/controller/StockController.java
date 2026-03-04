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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
        String normalized = stockService.normalizeTicker(ticker);
        return stockService.findStockByTicker(normalized)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/price/{ticker:.+}")
    @Operation(summary = "Get stock price by ticker", description = "Returns a numeric JSON value (double)")
    public ResponseEntity<Double> getStockPrice(@PathVariable String ticker) {
        String normalized = stockService.normalizeTicker(ticker);
        Stock stock = stockService.findStockByTicker(normalized)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No stock with ticker symbol " + normalized + " exists"));
        return ResponseEntity.ok(stock.price());
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
