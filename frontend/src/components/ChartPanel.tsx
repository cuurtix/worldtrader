import { createChart, type IPriceLine, type ISeriesApi } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

export function ChartPanel() {
  const ref = useRef<HTMLDivElement>(null);
  const chartRef = useRef<ReturnType<typeof createChart> | null>(null);
  const candleRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const linesRef = useRef<{ entry?: IPriceLine; sl?: IPriceLine; tp?: IPriceLine }>({});
  const [legend, setLegend] = useState('');
  const candles = useTradingStore((s) => s.candles);
  const symbol = useTradingStore((s) => s.symbol);
  const bracket = useTradingStore((s) => s.brackets[symbol]);

  useEffect(() => {
    if (!ref.current) return;
    const chart = createChart(ref.current, { height: 640, layout: { background: { color: '#141b2d' }, textColor: '#d8e4ff' }, grid: { vertLines: { color: '#1e283c' }, horzLines: { color: '#1e283c' } } });
    chartRef.current = chart;
    candleRef.current = chart.addCandlestickSeries({ upColor: '#00c076', downColor: '#ff4f4f', wickUpColor: '#00c076', wickDownColor: '#ff4f4f' });
    volRef.current = chart.addHistogramSeries({ priceScaleId: '', color: '#4369f6', priceFormat: { type: 'volume' } });

    chart.subscribeCrosshairMove((p) => {
      const d = p.seriesData.get(candleRef.current!);
      if (!d) return;
      const c = d as { open:number; high:number; low:number; close:number };
      setLegend(`O ${c.open.toFixed(2)} H ${c.high.toFixed(2)} L ${c.low.toFixed(2)} C ${c.close.toFixed(2)}`);
    });

    return () => chart.remove();
  }, []);

  useEffect(() => {
    if (!candleRef.current) return;
    candleRef.current.setData(candles);
    volRef.current?.setData(candles.map((c) => ({ time: c.time, value: c.volume, color: c.close >= c.open ? '#00c07688' : '#ff4f4f88' })));
  }, [candles]);

  useEffect(() => {
    if (!candleRef.current) return;
    if (linesRef.current.entry) candleRef.current.removePriceLine(linesRef.current.entry);
    if (linesRef.current.sl) candleRef.current.removePriceLine(linesRef.current.sl);
    if (linesRef.current.tp) candleRef.current.removePriceLine(linesRef.current.tp);
    if (bracket?.entry) linesRef.current.entry = candleRef.current.createPriceLine({ price: bracket.entry, color: '#8aa0c8', title: 'ENTRY', axisLabelVisible: true });
    if (bracket?.sl) linesRef.current.sl = candleRef.current.createPriceLine({ price: bracket.sl, color: '#ff4f4f', title: 'SL', axisLabelVisible: true });
    if (bracket?.tp) linesRef.current.tp = candleRef.current.createPriceLine({ price: bracket.tp, color: '#00c076', title: 'TP', axisLabelVisible: true });
  }, [bracket]);

  return <div className="panel"><div className="row"><h3>Chart · {symbol}</h3><div className="small">{legend}</div></div><div ref={ref} /><div className="small">Candle time uses UNIX seconds.</div></div>;
}
