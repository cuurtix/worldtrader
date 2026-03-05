import { useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

export function TradingPanel() {
  const [side, setSide] = useState<'BUY'|'SELL'>('BUY');
  const [type, setType] = useState<'MARKET'|'LIMIT'>('MARKET');
  const [qty, setQty] = useState(10);
  const [price, setPrice] = useState(0);
  const [cashAmount, setCashAmount] = useState(1000);
  const [macroOpen, setMacroOpen] = useState(false);
  const [macro, setMacro] = useState({ centralBank: 0.5, liquidity: 0.8, newsIntensity: 0.2, politicalStress: 0.1, riskOnOff: 0.5 });
  const symbol = useTradingStore((s) => s.symbol);
  const portfolio = useTradingStore((s) => s.portfolio);
  const place = useTradingStore((s) => s.place);
  const cashOp = useTradingStore((s) => s.cashOp);
  const applyMacro = useTradingStore((s) => s.applyMacro);
  const bracket = useTradingStore((s) => s.brackets[symbol]);
  const setBracket = useTradingStore((s) => s.setBracket);

  return <div className="panel"><h3>Order Ticket</h3><div className="row"><button onClick={()=>setSide('BUY')}>BUY</button><button onClick={()=>setSide('SELL')}>SELL</button></div><br/><select value={type} onChange={(e)=>setType(e.target.value as 'MARKET'|'LIMIT')}><option>MARKET</option><option>LIMIT</option></select><br/><br/><input type="number" value={qty} onChange={(e)=>setQty(Number(e.target.value))}/><br/><br/>{type==='LIMIT' && <><input type="number" value={price} onChange={(e)=>setPrice(Number(e.target.value))}/><br/><br/></>}<button onClick={()=>place(side, type, qty, type==='LIMIT'?price:undefined)}>Place Order {symbol}</button><hr/><h3>Account</h3><div className="stat">Balance: {portfolio?.cash.toFixed(2)}</div><div className="stat">Realized PnL: {portfolio?.realizedPnl.toFixed(2)}</div><div className="stat">Unrealized PnL: {portfolio?.unrealizedPnl.toFixed(2)}</div><div className="row"><input type="number" value={cashAmount} onChange={(e)=>setCashAmount(Number(e.target.value))}/><button onClick={()=>cashOp('deposit', cashAmount)}>Deposit</button><button onClick={()=>cashOp('withdraw', cashAmount)}>Withdraw</button></div><hr/><h3>Positions</h3>{portfolio?.positions.map((p)=><div key={p.ticker} className="stat"><b>{p.ticker}</b> qty={p.qty} avg={p.avgCost.toFixed(2)}<div className="row"><input aria-label="sl-input" placeholder="SL" value={bracket?.sl ?? ''} onChange={(e)=>setBracket(symbol, { sl: Number(e.target.value) || undefined })}/><input aria-label="tp-input" placeholder="TP" value={bracket?.tp ?? ''} onChange={(e)=>setBracket(symbol, { tp: Number(e.target.value) || undefined })}/></div></div>)}<button onClick={()=>setMacroOpen(true)}>Macro Panel</button>{macroOpen && <div className="panel"><h4>Macro Gameplay</h4>{Object.keys(macro).map((k)=> <div key={k}><label className="small">{k}</label><input type="range" min={0} max={1} step={0.01} value={macro[k as keyof typeof macro]} onChange={(e)=>setMacro((m)=>({...m,[k]:Number(e.target.value)}))}/></div>)}<div className="row"><button onClick={()=>applyMacro(macro)}>Apply</button><button onClick={()=>setMacroOpen(false)}>Close</button></div></div>}</div>;
}
