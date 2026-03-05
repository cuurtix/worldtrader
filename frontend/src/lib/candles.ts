import type { Candle, Trade } from './types';

export function buildCandlesFromTrades(trades: Trade[], timeframeSec: number): Candle[] {
  const buckets = new Map<number, Candle>();
  const sorted = [...trades].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());

  for (const t of sorted) {
    const epochSec = Math.floor(new Date(t.timestamp).getTime() / 1000);
    const bucket = Math.floor(epochSec / timeframeSec) * timeframeSec;
    const existing = buckets.get(bucket);
    if (!existing) {
      buckets.set(bucket, { time: bucket, open: t.price, high: t.price, low: t.price, close: t.price, volume: t.qty });
      continue;
    }
    existing.high = Math.max(existing.high, t.price);
    existing.low = Math.min(existing.low, t.price);
    existing.close = t.price;
    existing.volume += t.qty;
  }

  return [...buckets.values()].sort((a, b) => a.time - b.time);
}
