import { createChart, type IPriceLine, type ISeriesApi } from 'lightweight-charts';
import { useEffect, useRef, useState } from 'react';
import { useTradingStore } from '../store/useTradingStore';

export function ChartPanel() {
  const ref = useRef<HTMLDivElement>(null);
  const candleRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const linesRef = useRef<{ entry?: IPriceLine; sl?: IPriceLine; tp?: IPriceLine }>({});
  const dragging = useRef<'sl' | 'tp' | null>(null);
  const [legend, setLegend] = useState('');
  const candles = useTradingStore((s) => s.candles);
  const symbol = useTradingStore((s) => s.symbol);
  const bracket = useTradingStore((s) => s.brackets[symbol]);
  const setBracket = useTradingStore((s) => s.setBracket);

  useEffect(() => {
    if (!ref.current) return;
    const chart = createChart(ref.current, { height: 640, layout: { background: { color: '#141b2d' }, textColor: '#d8e4ff' }, grid: { vertLines: { color: '#1e283c' }, horzLines: { color: '#1e283c' } } });
    candleRef.current = chart.addCandlestickSeries({ upColor: '#00c076', downColor: '#ff4f4f', wickUpColor: '#00c076', wickDownColor: '#ff4f4f' });
    volRef.current = chart.addHistogramSeries({ priceScaleId: '', color: '#4369f6', priceFormat: { type: 'volume' } });

    chart.subscribeCrosshairMove((p) => {
      const d = p.seriesData.get(candleRef.current!);
      if (!d) return;
      const c = d as { open:number; high:number; low:number; close:number };
      setLegend(`O ${c.open.toFixed(2)} H ${c.high.toFixed(2)} L ${c.low.toFixed(2)} C ${c.close.toFixed(2)}`);
    });

    chart.subscribeClick((param) => {
      if (!param.point || !candleRef.current || !bracket) return;
      const y = param.point.y;
      for (const key of ['sl', 'tp'] as const) {
        const p = bracket[key];
        if (!p) continue;
        const py = candleRef.current.priceToCoordinate(p);
        if (py != null && Math.abs(py - y) <= 8) dragging.current = key;
      }
    });

    const onMove = (ev: MouseEvent) => {
      if (!dragging.current || !candleRef.current || !ref.current) return;
      const rect = ref.current.getBoundingClientRect();
      const y = ev.clientY - rect.top;
      const newPrice = candleRef.current.coordinateToPrice(y);
      if (newPrice) {
        setBracket(symbol, { [dragging.current]: Number(newPrice.toFixed(2)) });
      }
    };
    const onUp = () => { dragging.current = null; };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      chart.remove();
    };
  }, [setBracket, symbol, bracket]);

  useEffect(() => {
    candleRef.current?.setData(candles);
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

  return <div className="panel"><div className="row"><h3>Chart</h3><div className="small">{legend}</div></div><div ref={ref} /><div className="small">SL/TP lines draggable around label height.</div></div>;
}
