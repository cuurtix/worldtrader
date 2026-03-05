import { useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

export function Sidebar() {
  const [q, setQ] = useState('');
  const symbol = useTradingStore((s) => s.symbol);
  const setSymbol = useTradingStore((s) => s.setSymbol);
  const watchlist = useTradingStore((s) => s.watchlist);
  const availableSymbols = useTradingStore((s) => s.availableSymbols);

  const symbols = availableSymbols.filter((s) => s.toLowerCase().includes(q.toLowerCase()));
  return <div className="panel"><h3>Markets</h3><input placeholder="Search" value={q} onChange={(e)=>setQ(e.target.value)}/><h4>Simulation Universe</h4>{symbols.map((s)=><div key={s} className={`list-item ${symbol===s?'active':''}`} onClick={()=>setSymbol(s)}>{s}</div>)}<h4>Watchlist</h4>{watchlist.map((s)=><div key={s} className="small">• {s}</div>)}</div>;
}
