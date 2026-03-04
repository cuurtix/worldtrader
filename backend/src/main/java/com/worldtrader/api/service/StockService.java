package com.worldtrader.api.service;

import com.worldtrader.api.exception.StockNotFoundException;
import com.worldtrader.api.market.service.MarketSimulationService;
import com.worldtrader.api.model.Stock;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StockService {

    private final MarketSimulationService market;

    public StockService(MarketSimulationService market) {
        this.market = market;
    }

    public List<Stock> getAllStocks() {
        return market.tickers().stream().sorted().map(this::toStock).toList();
    }

    public Optional<Stock> findStockByTicker(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        if (!market.tickers().contains(ticker)) {
            return Optional.empty();
        }
        return Optional.of(toStock(ticker));
    }

    public Stock getStockByTicker(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        return findStockByTicker(ticker).orElseThrow(() -> new StockNotFoundException(ticker));
    }

    public double getPriceByTicker(String rawTicker) {
        return getStockByTicker(rawTicker).price();
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

    private Stock toStock(String ticker) {
        return new Stock(ticker, market.companyName(ticker), market.getLastPrice(ticker));
    }
}
