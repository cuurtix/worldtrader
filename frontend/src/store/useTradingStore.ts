import { create } from 'zustand';
import { adjustBalance, apiBaseUrl, fetchCandles, fetchMarketStatus, fetchOrderBook, fetchPortfolio, fetchStocks, fetchTradesV2, pauseMarket, placeOrder, resumeMarket, updateRegime } from '../lib/api';
import { mapCandleDto, mapOrderBook, mapTradeDto } from '../lib/adapters';
import type { Candle, OrderBook, OrderType, Portfolio, Side, TradeTick } from '../lib/types';

interface Bracket { entry?: number; sl?: number; tp?: number }
interface DebugState {
  apiBaseUrl: string;
  ticker: string;
  candlesCount: number;
  candlesSample?: Candle;
  tradesCount: number;
  tradeSample?: TradeTick;
  orderbookLevels: number;
  accountCash?: number;
  latencyMs?: number;
  error?: string;
}

interface TradingState {
  symbol: string;
  traderId: string;
  timeframe: '1s' | '5s' | '1m';
  candles: Candle[];
  portfolio?: Portfolio;
  watchlist: string[];
  availableSymbols: string[];
  trades: TradeTick[];
  orderbook?: OrderBook;
  marketRunning: boolean;
  brackets: Record<string, Bracket>;
  loading: boolean;
  debug: DebugState;
  loadData: () => Promise<void>;
  setSymbol: (symbol: string) => void;
  place: (side: Side, type: OrderType, qty: number, price?: number) => Promise<void>;
  cashOp: (mode: 'deposit'|'withdraw', amount: number) => Promise<void>;
  setTimeframe: (tf: '1s' | '5s' | '1m') => void;
  setBracket: (symbol: string, patch: Partial<Bracket>) => void;
  applyMacro: (vals: { centralBank: number; liquidity: number; newsIntensity: number; politicalStress: number; riskOnOff: number; }) => Promise<void>;
  toggleMarketRunning: () => Promise<void>;
}

export const useTradingStore = create<TradingState>((set, get) => ({
  symbol: 'AAPL', traderId: 'retail_player', timeframe: '1s', candles: [], watchlist: ['AAPL', 'MSFT', 'TSLA'], availableSymbols: ['AAPL', 'MSFT', 'TSLA'], trades: [], marketRunning: true,
  loading: false, brackets: {}, debug: { apiBaseUrl: apiBaseUrl(), ticker: 'AAPL', candlesCount: 0, tradesCount: 0, orderbookLevels: 0 },
  async loadData() {
    set({ loading: true });
    const startedAt = performance.now();
    try {
      const [stocks, portfolio, marketStatus] = await Promise.all([fetchStocks(), fetchPortfolio(get().traderId), fetchMarketStatus()]);
      const availableSymbols = stocks.map((s) => s.ticker);
      const nextSymbol = availableSymbols.includes(get().symbol) ? get().symbol : availableSymbols[0] ?? get().symbol;
      const [candlesDto, tradesDto, rawOrderbook] = await Promise.all([
        fetchCandles(nextSymbol, get().timeframe, 500),
        fetchTradesV2(nextSymbol, 200),
        fetchOrderBook(nextSymbol, 20)
      ]);
      const candles = candlesDto.map(mapCandleDto).sort((a, b) => a.time - b.time);
      const trades = tradesDto.map(mapTradeDto);
      const orderbook = mapOrderBook(rawOrderbook);
      const last = candles.at(-1)?.close;
      const latencyMs = Math.round(performance.now() - startedAt);
      console.info('[trading] snapshot', {
        apiBaseUrl: apiBaseUrl(), ticker: nextSymbol, timeframe: get().timeframe,
        candles: { count: candles.length, sample: candles.slice(-2) },
        trades: { count: trades.length, sample: trades.slice(0, 2) },
        orderbook: { bids: orderbook.bids.length, asks: orderbook.asks.length },
        account: { cash: portfolio.cash, positions: portfolio.positions.length }
      });
      set((state) => ({
        symbol: nextSymbol,
        candles,
        trades,
        orderbook,
        portfolio,
        marketRunning: marketStatus.running,
        availableSymbols,
        watchlist: availableSymbols,
        loading: false,
        debug: {
          apiBaseUrl: apiBaseUrl(),
          ticker: nextSymbol,
          candlesCount: candles.length,
          candlesSample: candles.at(-1),
          tradesCount: trades.length,
          tradeSample: trades[0],
          orderbookLevels: orderbook.bids.length + orderbook.asks.length,
          accountCash: portfolio.cash,
          latencyMs
        },
        brackets: { ...state.brackets, [nextSymbol]: { ...state.brackets[nextSymbol], entry: state.brackets[nextSymbol]?.entry ?? last } }
      }));
    } catch (error) {
      console.error('[trading] loadData failed', error);
      set({ loading: false, debug: { ...get().debug, error: String(error) } });
    }
  },
  setSymbol(symbol) {
    if (!get().availableSymbols.includes(symbol)) return;
    set({ symbol, debug: { ...get().debug, ticker: symbol } });
    void get().loadData();
  },
  async place(side, type, qty, price) {
    await placeOrder({ traderId: get().traderId, ticker: get().symbol, side, type, qty, price });
    await get().loadData();
  },
  async cashOp(mode, amount) { const portfolio = await adjustBalance(get().traderId, amount, mode); set({ portfolio }); },
  setTimeframe(tf) { set({ timeframe: tf }); void get().loadData(); },
  setBracket(symbol, patch) { set((s) => ({ brackets: { ...s.brackets, [symbol]: { ...s.brackets[symbol], ...patch } } })); },
  async applyMacro(vals) { await updateRegime(vals); },
  async toggleMarketRunning() {
    if (get().marketRunning) await pauseMarket();
    else await resumeMarket();
    await get().loadData();
  }
}));
