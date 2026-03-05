import type { CandleDto, MarketStatus, OrderBook, OrderType, Portfolio, Side, Stock, Trade, TradeTick } from './types';

const API = '/api/v1';

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const startedAt = performance.now();
  const response = await fetch(`${API}${path}`, init);
  const elapsed = Math.round(performance.now() - startedAt);
  const text = await response.text();
  if (!response.ok) {
    console.error('[api] error', { path, status: response.status, body: text, elapsedMs: elapsed });
    throw new Error(`API ${path} failed with status=${response.status}`);
  }
  const data = text ? JSON.parse(text) : null;
  console.debug('[api] ok', { path, elapsedMs: elapsed });
  return data as T;
}

export function apiBaseUrl(): string {
  return API;
}

export async function fetchTrades(symbol: string, limit = 500): Promise<Trade[]> {
  return apiFetch<Trade[]>(`/trades/${symbol}?limit=${limit}`);
}

export async function fetchTradesV2(symbol: string, limit = 200): Promise<TradeTick[]> {
  return apiFetch<TradeTick[]>(`/trades?ticker=${symbol}&limit=${limit}`);
}

export async function fetchCandles(symbol: string, timeframe: '1s' | '5s' | '1m', limit = 500): Promise<CandleDto[]> {
  return apiFetch<CandleDto[]>(`/candles?ticker=${symbol}&tf=${timeframe}&limit=${limit}`);
}

export async function fetchStocks(): Promise<Stock[]> {
  return apiFetch<Stock[]>('/stocks');
}

export async function fetchPortfolio(traderId: string): Promise<Portfolio> {
  return apiFetch<Portfolio>(`/portfolio/${traderId}`);
}

export async function fetchOrderBook(ticker: string, levels = 20): Promise<OrderBook> {
  return apiFetch<OrderBook>(`/orderbook/${ticker}?levels=${levels}`);
}

export async function fetchMarketStatus(): Promise<MarketStatus> {
  return apiFetch<MarketStatus>('/market');
}

export async function placeOrder(payload: { traderId: string; ticker: string; side: Side; type: OrderType; qty: number; price?: number; }) {
  return apiFetch(`/orders`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...payload, tif: payload.type === 'LIMIT' ? 'GTC' : 'IOC' }) });
}

export async function adjustBalance(traderId: string, amount: number, mode: 'deposit'|'withdraw') {
  return apiFetch<Portfolio>(`/portfolio/${traderId}/${mode}?amount=${amount}`, { method: 'POST' });
}

export async function updateRegime(regime: Record<string, number>) {
  const current = await apiFetch<Record<string, number>>('/regime');
  return apiFetch(`/regime`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...current, ...regime }) });
}

export async function pauseMarket() { return apiFetch('/market/pause', { method: 'POST' }); }
export async function resumeMarket() { return apiFetch('/market/resume', { method: 'POST' }); }
