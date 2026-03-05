import { describe, expect, it } from 'vitest';
import { buildCandlesFromTrades } from '../src/lib/candles';

describe('buildCandlesFromTrades', () => {
  it('aggregates ohlc and volume by bucket', () => {
    const candles = buildCandlesFromTrades([
      { tradeId: '1', ticker: 'AAPL', price: 100, qty: 2, timestamp: '2024-01-01T00:00:01Z' },
      { tradeId: '2', ticker: 'AAPL', price: 105, qty: 3, timestamp: '2024-01-01T00:00:10Z' },
      { tradeId: '3', ticker: 'AAPL', price: 99, qty: 1, timestamp: '2024-01-01T00:00:59Z' }
    ], 60);

    expect(candles).toHaveLength(1);
    expect(candles[0]).toMatchObject({ open: 100, high: 105, low: 99, close: 99, volume: 6 });
  });
});
