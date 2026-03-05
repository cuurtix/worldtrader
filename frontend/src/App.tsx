import { useEffect, useRef } from 'react';
import { Sidebar } from './components/Sidebar';
import { ChartPanel } from './components/ChartPanel';
import { TradingPanel } from './components/TradingPanel';
import { useTradingStore } from './store/useTradingStore';

export function App() {
  const loadData = useTradingStore((s) => s.loadData);
  const candles = useTradingStore((s) => s.candles);
  const symbol = useTradingStore((s) => s.symbol);
  const bracket = useTradingStore((s) => s.brackets[symbol]);
  const portfolio = useTradingStore((s) => s.portfolio);
  const place = useTradingStore((s) => s.place);
  const guard = useRef(false);

  useEffect(() => {
    void loadData();
    const id = window.setInterval(() => void loadData(), 1000);
    return () => window.clearInterval(id);
  }, [loadData]);

  useEffect(() => {
    const last = candles.at(-1)?.close;
    const pos = portfolio?.positions.find((p) => p.ticker === symbol);
    if (!last || !pos || pos.qty === 0 || guard.current) return;

    if (pos.qty > 0 && ((bracket?.sl && last <= bracket.sl) || (bracket?.tp && last >= bracket.tp))) {
      guard.current = true;
      void place('SELL', 'MARKET', Math.abs(pos.qty)).finally(() => { guard.current = false; });
    }
    if (pos.qty < 0 && ((bracket?.sl && last >= bracket.sl) || (bracket?.tp && last <= bracket.tp))) {
      guard.current = true;
      void place('BUY', 'MARKET', Math.abs(pos.qty)).finally(() => { guard.current = false; });
    }
  }, [candles, portfolio, symbol, bracket, place]);

  return <div className="layout"><Sidebar/><ChartPanel/><TradingPanel/></div>;
}
