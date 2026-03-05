import { useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

export function Sidebar() {
  const [q, setQ] = useState('');
  const symbol = useTradingStore((s) => s.symbol);
  const setSymbol = useTradingStore((s) => s.setSymbol);
  const watchlist = useTradingStore((s) => s.watchlist);
  const availableSymbols = useTradingStore((s) => s.availableSymbols);
  const trades = useTradingStore((s) => s.trades);
  const orderbook = useTradingStore((s) => s.orderbook);

  const symbols = availableSymbols.filter((s) => s.toLowerCase().includes(q.toLowerCase()));
  return <div className="panel"><h3>Markets</h3><input placeholder="Search" value={q} onChange={(e)=>setQ(e.target.value)}/><h4>Simulation Universe</h4>{symbols.map((s)=><div key={s} className={`list-item ${symbol===s?'active':''}`} onClick={()=>setSymbol(s)}>{s}</div>)}<h4>Watchlist</h4>{watchlist.map((s)=><div key={s} className="small">• {s}</div>)}<h4>OrderBook</h4><div className="small">Bid {orderbook?.bestBid?.toFixed(2)} / Ask {orderbook?.bestAsk?.toFixed(2)}</div>{orderbook?.bids.slice(0, 5).map((l) => <div key={`b-${l.price}`} className="small">B {l.price.toFixed(2)} x {l.qty}</div>)}{orderbook?.asks.slice(0, 5).map((l) => <div key={`a-${l.price}`} className="small">A {l.price.toFixed(2)} x {l.qty}</div>)}<h4>Trades Tape</h4>{trades.slice(0, 8).map((t, i)=><div key={`${t.t}-${i}`} className="small">{new Date(t.t * 1000).toLocaleTimeString()} {t.side} {t.qty}@{t.price.toFixed(2)}</div>)}</div>;
}
