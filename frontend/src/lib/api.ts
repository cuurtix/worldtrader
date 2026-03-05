import type { OrderType, Portfolio, Side, Stock, Trade } from './types';

const API = '/api/v1';

export async function fetchTrades(symbol: string, limit = 500): Promise<Trade[]> {
  const r = await fetch(`${API}/trades/${symbol}?limit=${limit}`);
  if (!r.ok) throw new Error('trades fetch failed');
  return r.json();
}

export async function fetchStocks(): Promise<Stock[]> {
  const r = await fetch(`${API}/stocks`);
  if (!r.ok) throw new Error('stocks fetch failed');
  return r.json();
}

export async function fetchPortfolio(traderId: string): Promise<Portfolio> {
  const r = await fetch(`${API}/portfolio/${traderId}`);
  if (!r.ok) throw new Error('portfolio fetch failed');
  return r.json();
}

export async function placeOrder(payload: { traderId: string; ticker: string; side: Side; type: OrderType; qty: number; price?: number; }) {
  return fetch(`${API}/orders`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...payload, tif: payload.type === 'LIMIT' ? 'GTC' : 'IOC' }) });
}

export async function adjustBalance(traderId: string, amount: number, mode: 'deposit'|'withdraw') {
  const r = await fetch(`${API}/portfolio/${traderId}/${mode}?amount=${amount}`, { method: 'POST' });
  if (!r.ok) throw new Error(`${mode} failed`);
  return r.json();
}

export async function updateRegime(regime: Record<string, number>) {
  const r0 = await fetch(`${API}/regime`);
  const current = r0.ok ? await r0.json() : {};
  return fetch(`${API}/regime`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...current, ...regime }) });
}
