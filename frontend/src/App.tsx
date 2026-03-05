import { useEffect, useRef } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Sidebar } from './components/Sidebar';
import { ChartPanel } from './components/ChartPanel';
import { TradingPanel } from './components/TradingPanel';
import { useTradingStore } from './store/useTradingStore';

function MainPage() {
  const loadData = useTradingStore((s) => s.loadData);
  const candles = useTradingStore((s) => s.candles);
  const symbol = useTradingStore((s) => s.symbol);
  const bracket = useTradingStore((s) => s.brackets[symbol]);
  const portfolio = useTradingStore((s) => s.portfolio);
  const place = useTradingStore((s) => s.place);
  const guard = useRef(false);

  useEffect(() => {
    void loadData();
    const id = window.setInterval(() => void loadData(), 750);
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

function DebugPage() {
  const debug = useTradingStore((s) => s.debug);
  const marketRunning = useTradingStore((s) => s.marketRunning);
  const timeframe = useTradingStore((s) => s.timeframe);

  return <div className="panel"><h2>Debug</h2><div className="stat">API: {debug.apiBaseUrl}</div><div className="stat">Ticker: {debug.ticker}</div><div className="stat">Market running: {String(marketRunning)}</div><div className="stat">Timeframe: {timeframe}</div><div className="stat">Latency: {debug.latencyMs}ms</div><div className="stat">Candles: {debug.candlesCount}</div><pre>{JSON.stringify(debug.candlesSample, null, 2)}</pre><div className="stat">Trades: {debug.tradesCount}</div><pre>{JSON.stringify(debug.tradeSample, null, 2)}</pre><div className="stat">Orderbook levels: {debug.orderbookLevels}</div><div className="stat">Account cash: {debug.accountCash}</div><div className="stat">Error: {debug.error ?? 'none'}</div></div>;
}

export function App() {
  return <Routes><Route path="/" element={<MainPage/>} /><Route path="/debug" element={<DebugPage/>} /><Route path="*" element={<Navigate to="/" replace />} /></Routes>;
}
