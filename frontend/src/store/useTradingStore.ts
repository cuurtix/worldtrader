import { create } from 'zustand';
import { adjustBalance, fetchPortfolio, fetchStocks, fetchTrades, placeOrder, updateRegime } from '../lib/api';
import { buildCandlesFromTrades } from '../lib/candles';
import type { Candle, OrderType, Portfolio, Side } from '../lib/types';

interface Bracket { entry?: number; sl?: number; tp?: number }

interface TradingState {
  symbol: string;
  traderId: string;
  timeframeSec: number;
  candles: Candle[];
  portfolio?: Portfolio;
  watchlist: string[];
  availableSymbols: string[];
  brackets: Record<string, Bracket>;
  loading: boolean;
  loadData: () => Promise<void>;
  setSymbol: (symbol: string) => void;
  place: (side: Side, type: OrderType, qty: number, price?: number) => Promise<void>;
  cashOp: (mode: 'deposit'|'withdraw', amount: number) => Promise<void>;
  setTimeframe: (sec: number) => void;
  setBracket: (symbol: string, patch: Partial<Bracket>) => void;
  applyMacro: (vals: { centralBank: number; liquidity: number; newsIntensity: number; politicalStress: number; riskOnOff: number; }) => Promise<void>;
}

export const useTradingStore = create<TradingState>((set, get) => ({
  symbol: 'AAPL', traderId: 'retail_player', timeframeSec: 60, candles: [], watchlist: ['AAPL', 'MSFT', 'TSLA'], availableSymbols: ['AAPL', 'MSFT', 'TSLA'], loading: false, brackets: {},
  async loadData() {
    set({ loading: true });
    try {
      const [stocks, portfolio] = await Promise.all([fetchStocks(), fetchPortfolio(get().traderId)]);
      const availableSymbols = stocks.map((s) => s.ticker);
      const nextSymbol = availableSymbols.includes(get().symbol) ? get().symbol : availableSymbols[0] ?? get().symbol;
      const trades = await fetchTrades(nextSymbol);
      const candles = buildCandlesFromTrades(trades, get().timeframeSec);
      const last = candles.at(-1)?.close;
      set((state) => ({
        symbol: nextSymbol,
        candles,
        portfolio,
        availableSymbols,
        watchlist: availableSymbols,
        loading: false,
        brackets: { ...state.brackets, [nextSymbol]: { ...state.brackets[nextSymbol], entry: state.brackets[nextSymbol]?.entry ?? last } }
      }));
    } catch {
      set({ loading: false });
    }
  },
  setSymbol(symbol) {
    if (!get().availableSymbols.includes(symbol)) return;
    set({ symbol });
    void get().loadData();
  },
  async place(side, type, qty, price) { await placeOrder({ traderId: get().traderId, ticker: get().symbol, side, type, qty, price }); await get().loadData(); },
  async cashOp(mode, amount) { const portfolio = await adjustBalance(get().traderId, amount, mode); set({ portfolio }); },
  setTimeframe(sec) { set({ timeframeSec: sec }); void get().loadData(); },
  setBracket(symbol, patch) { set((s) => ({ brackets: { ...s.brackets, [symbol]: { ...s.brackets[symbol], ...patch } } })); },
  async applyMacro(vals) { await updateRegime(vals); }
}));
