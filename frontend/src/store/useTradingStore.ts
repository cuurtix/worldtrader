import { create } from 'zustand';
import { adjustBalance, fetchPortfolio, fetchTrades, placeOrder, updateRegime } from '../lib/api';
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
  symbol: 'AAPL', traderId: 'retail_player', timeframeSec: 60, candles: [], watchlist: ['AAPL', 'MSFT', 'TSLA', 'BTCUSD', 'EURUSD'], loading: false, brackets: {},
  async loadData() {
    set({ loading: true });
    const [trades, portfolio] = await Promise.all([fetchTrades(get().symbol), fetchPortfolio(get().traderId)]);
    const candles = buildCandlesFromTrades(trades, get().timeframeSec);
    const last = candles.at(-1)?.close;
    set((state) => ({ candles, portfolio, loading: false, brackets: { ...state.brackets, [get().symbol]: { ...state.brackets[get().symbol], entry: state.brackets[get().symbol]?.entry ?? last } } }));
  },
  setSymbol(symbol) { set({ symbol }); void get().loadData(); },
  async place(side, type, qty, price) { await placeOrder({ traderId: get().traderId, ticker: get().symbol, side, type, qty, price }); await get().loadData(); },
  async cashOp(mode, amount) { const portfolio = await adjustBalance(get().traderId, amount, mode); set({ portfolio }); },
  setTimeframe(sec) { set({ timeframeSec: sec }); void get().loadData(); },
  setBracket(symbol, patch) { set((s) => ({ brackets: { ...s.brackets, [symbol]: { ...s.brackets[symbol], ...patch } } })); },
  async applyMacro(vals) { await updateRegime(vals); }
}));
