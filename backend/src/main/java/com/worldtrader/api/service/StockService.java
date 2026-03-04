package com.worldtrader.api.service;

import com.worldtrader.api.exception.StockNotFoundException;
import com.worldtrader.api.model.Stock;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StockService {

    private final Map<String, Stock> stocks = new LinkedHashMap<>();

    public StockService() {
        seedInMemoryStocks();
    }

    private void seedInMemoryStocks() {
        upsert(new Stock("AAPL", 190.12));
        upsert(new Stock("MSFT", 421.33));
        upsert(new Stock("TSLA", 178.44));
        upsert(new Stock("NVDA", 116.78));
        upsert(new Stock("BRK.B", 412.05));
    }

    public List<Stock> getAllStocks() {
        return List.copyOf(stocks.values());
    }

    public Stock getStockByTicker(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        Stock stock = stocks.get(ticker);
        if (stock == null) {
            throw new StockNotFoundException(ticker);
        }
        return stock;
    }

    public double getPriceByTicker(String rawTicker) {
        return getStockByTicker(rawTicker).price();
    }

    public Map<String, Double> getPricesByTickers(String rawTickersCsv) {
        if (rawTickersCsv == null || rawTickersCsv.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'tickers' is required");
        }
        Map<String, Double> out = new LinkedHashMap<>();
        for (String token : rawTickersCsv.split(",")) {
            String ticker = normalizeTicker(token);
            out.put(ticker, getPriceByTicker(ticker));
        }
        return out;
    }

    public Stock getRandomStock() {
        List<Stock> all = getAllStocks();
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }

    public String normalizeTicker(String rawTicker) {
        if (rawTicker == null) {
            throw new IllegalArgumentException("ticker cannot be null");
        }
        String ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
        if (ticker.isEmpty()) {
            throw new IllegalArgumentException("ticker cannot be blank");
        }
        return ticker;
    }

    private void upsert(Stock stock) {
        stocks.put(normalizeTicker(stock.ticker()), new Stock(normalizeTicker(stock.ticker()), stock.price()));
    }
}
