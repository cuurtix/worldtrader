import { useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

const groups: Record<string, string[]> = {
  EQUITIES: ['AAPL', 'MSFT', 'TSLA'], FOREX: ['EURUSD', 'GBPUSD'], CRYPTO: ['BTCUSD', 'ETHUSD'], INDICES: ['SPX', 'NDX'], COMMODITIES: ['XAUUSD', 'WTI']
};

export function Sidebar() {
  const [q, setQ] = useState('');
  const [group, setGroup] = useState('EQUITIES');
  const symbol = useTradingStore((s) => s.symbol);
  const setSymbol = useTradingStore((s) => s.setSymbol);
  const watchlist = useTradingStore((s) => s.watchlist);

  const symbols = groups[group].filter((s) => s.toLowerCase().includes(q.toLowerCase()));
  return <div className="panel"><h3>Markets</h3><div className="row">{Object.keys(groups).map((g) => <button key={g} onClick={() => setGroup(g)}>{g}</button>)}</div><br/><input placeholder="Search" value={q} onChange={(e)=>setQ(e.target.value)}/><h4>{group}</h4>{symbols.map((s)=><div key={s} className={`list-item ${symbol===s?'active':''}`} onClick={()=>setSymbol(s)}>{s}</div>)}<h4>Watchlist</h4>{watchlist.map((s)=><div key={s} className="small">• {s}</div>)}</div>;
}
