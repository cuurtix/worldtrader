export type Side = 'BUY' | 'SELL';
export type OrderType = 'MARKET' | 'LIMIT';

export interface Trade {
  tradeId: string;
  ticker: string;
  price: number;
  qty: number;
  timestamp: string;
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
