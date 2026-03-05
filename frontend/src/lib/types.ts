export type Side = 'BUY' | 'SELL';
export type OrderType = 'MARKET' | 'LIMIT';

export interface Trade {
  tradeId: string;
  ticker: string;
  price: number;
  qty: number;
  timestamp: string;
}

export interface TradeTick {
  t: number;
  price: number;
  qty: number;
  side: Side;
}

export interface CandleDto {
  t: number;
  o: number;
  h: number;
  l: number;
  c: number;
  v: number;
}

export interface Candle { time: number; open: number; high: number; low: number; close: number; volume: number; }

export interface Position { ticker: string; qty: number; avgCost: number; sl?: number; tp?: number; }

export interface Portfolio {
  traderId: string;
  cash: number;
  realizedPnl: number;
  unrealizedPnl: number;
  positions: Position[];
}

export interface Stock {
  ticker: string;
  companyName: string;
  price: number;
}

export interface OrderBook {
  ticker: string;
  bestBid: number | null;
  bestAsk: number | null;
  spread: number | null;
  bids: Array<{ price: number; qty: number }>;
  asks: Array<{ price: number; qty: number }>;
  imbalance: number;
}

export interface MarketStatus {
  running: boolean;
  intervalMillis: number;
  tickCount: number;
}
