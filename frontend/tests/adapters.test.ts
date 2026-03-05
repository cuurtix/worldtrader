import { describe, expect, it } from 'vitest';
import { mapCandleDto, toEpochSeconds } from '../src/lib/adapters';

describe('adapters', () => {
  it('maps candle dto to lightweight-chart format with epoch seconds', () => {
    const candle = mapCandleDto({ t: 1710000123, o: 100, h: 110, l: 95, c: 108, v: 42 });
    expect(candle).toEqual({ time: 1710000123, open: 100, high: 110, low: 95, close: 108, volume: 42 });
  });

  it('converts epoch milliseconds to seconds', () => {
    expect(toEpochSeconds(1710000123000)).toBe(1710000123);
  });
});
