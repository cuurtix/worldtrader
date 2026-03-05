import type { Candle, CandleDto, OrderBook, Trade, TradeTick } from './types';

export function toEpochSeconds(value: string | number): number {
  if (typeof value === 'number') {
    return value > 1e12 ? Math.floor(value / 1000) : Math.floor(value);
  }
  const ms = new Date(value).getTime();
  return Math.floor(ms / 1000);
}

export function mapCandleDto(input: CandleDto): Candle {
  return { time: toEpochSeconds(input.t), open: input.o, high: input.h, low: input.l, close: input.c, volume: input.v };
}

export function mapTradeDto(input: TradeTick | Trade): TradeTick {
  if ('timestamp' in input) {
    return { t: toEpochSeconds(input.timestamp), price: input.price, qty: input.qty, side: 'BUY' };
  }
  return { ...input, t: toEpochSeconds(input.t) };
}

export function mapOrderBook(input: OrderBook): OrderBook {
  return {
    ...input,
    bids: input.bids.slice(0, 12),
    asks: input.asks.slice(0, 12)
  };
}
