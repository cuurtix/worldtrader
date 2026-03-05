from __future__ import annotations

import asyncio
import csv
import math
import random
import time
import uuid
from collections import defaultdict, deque
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .models import MarketRegime, Order, OrderType, Portfolio, Sentiment, Side
from .orderbook import LimitOrderBook


@dataclass
class Event:
    kind: str
    payload: dict[str, Any]
    ts: float


class RealTimeMarket:
    def __init__(self, tps: int = 20, difficulty: str = "normal") -> None:
        self.tps = tps
        self.running = False
        self.assets = {"AAPL": 190.0, "BTC": 60000.0, "GOLD": 2350.0, "OIL": 78.0}
        self.books = {s: LimitOrderBook(s) for s in self.assets}
        self.event_q: asyncio.PriorityQueue[tuple[float, Event]] = asyncio.PriorityQueue()
        self.regime = MarketRegime()
        self.sentiment = Sentiment()
        self.player_id = "joueur"
        self.portfolios: dict[str, Portfolio] = defaultdict(Portfolio)
        self.recent_trades: dict[str, deque] = {s: deque(maxlen=4000) for s in self.assets}
        self.pending_orders: dict[str, Order] = {}
        self.ws_clients: set[Any] = set()
        self.agent_ids = [f"mm_{i}" for i in range(6)] + [f"noise_{i}" for i in range(120)] + [f"momentum_{i}" for i in range(40)] + [f"meanrev_{i}" for i in range(40)] + [f"fundamental_{i}" for i in range(30)] + [f"arb_{i}" for i in range(30)]
        self._rng = random.Random(42)
        self._difficulty = difficulty
        self.achievements: dict[str, bool] = {"premier_trade": False, "profit_1000": False}
        self.log_dir = Path("runs/realtime_logs")
        self.log_dir.mkdir(parents=True, exist_ok=True)
        self._init_books()

    def _init_books(self) -> None:
        now = time.time()
        for symbol, mid in self.assets.items():
            for i in range(1, 6):
                self.books[symbol].submit(Order(str(uuid.uuid4()), "seed", symbol, Side.BUY, 80, now, OrderType.LIMIT, round(mid - i * 0.1, 2)))
                self.books[symbol].submit(Order(str(uuid.uuid4()), "seed", symbol, Side.SELL, 80, now, OrderType.LIMIT, round(mid + i * 0.1, 2)))

    async def start(self) -> None:
        self.running = True
        asyncio.create_task(self._event_loop())
        asyncio.create_task(self._scheduler())

    async def _scheduler(self) -> None:
        while self.running:
            now = time.time()
            await self.event_q.put((now, Event("agent_decision", {}, now)))
            await asyncio.sleep(1 / self.tps)

    async def _event_loop(self) -> None:
        while self.running:
            _, evt = await self.event_q.get()
            if evt.kind == "agent_decision":
                self._step_agents()
            elif evt.kind == "new_order":
                self._handle_order(evt.payload["order"])
            elif evt.kind == "cancel_order":
                self._cancel(evt.payload["order_id"])

    def _step_agents(self) -> None:
        self._drift_regime()
        self._flash_crash()
        for symbol in self.assets:
            self._market_makers(symbol)
            self._flow_orders(symbol)

    def _drift_regime(self) -> None:
        x = self._rng.random()
        if x < 0.01:
            self.regime = MarketRegime("faible_volatilite", 0.0018, 0.01)
        elif x < 0.03:
            self.regime = MarketRegime("haute_volatilite", 0.01, 0.08)
        else:
            self.regime = MarketRegime("moyenne_volatilite", 0.0045, 0.03)
        self.sentiment.fear = min(1.0, max(0.0, self.sentiment.fear + self._rng.gauss(0, 0.02)))
        self.sentiment.greed = min(1.0, max(0.0, self.sentiment.greed + self._rng.gauss(0, 0.02)))
        self.sentiment.panic = max(0.0, min(1.0, self.sentiment.panic * 0.97 + (0.2 if self.regime.name == "haute_volatilite" else 0)))
        self.sentiment.confidence = max(0.0, min(1.0, 1.0 - self.sentiment.fear * 0.5))

    def _flash_crash(self) -> None:
        if self._rng.random() > 0.001:
            return
        symbol = self._rng.choice(list(self.assets))
        for _ in range(20):
            oid = str(uuid.uuid4())
            size = int(self._rng.paretovariate(2.2) * 180)
            self._handle_order(Order(oid, "flash_institutionnel", symbol, Side.SELL, size, time.time(), OrderType.MARKET))

    def _market_makers(self, symbol: str) -> None:
        bb = self.books[symbol].best_bid() or self.assets[symbol] * 0.999
        ba = self.books[symbol].best_ask() or self.assets[symbol] * 1.001
        mid = (bb + ba) / 2
        for i in range(6):
            mm = f"mm_{i}"
            inv = self.portfolios[mm].positions.get(symbol, 0)
            skew = -0.003 * inv
            spread = max(0.05, mid * (0.001 + self.regime.sigma * 1.6 + self.sentiment.panic * 0.002))
            size = min(300, int(self._rng.paretovariate(2.6) * 18))
            latency = self._rng.uniform(0.001, 0.05)
            if self._rng.random() < 0.4 + self.sentiment.panic * 0.5:
                self._cancel_random_from_agent(mm, symbol)
            if self.sentiment.panic > 0.85 and self._rng.random() < 0.5:
                continue
            bid = round(mid - spread / 2 + skew, 2)
            ask = round(mid + spread / 2 + skew, 2)
            self._schedule_order(Order(str(uuid.uuid4()), mm, symbol, Side.BUY, max(1, size), time.time(), OrderType.LIMIT, max(0.01, bid)), latency)
            self._schedule_order(Order(str(uuid.uuid4()), mm, symbol, Side.SELL, max(1, size), time.time(), OrderType.LIMIT, ask), latency)

    def _flow_orders(self, symbol: str) -> None:
        bb = self.books[symbol].best_bid() or self.assets[symbol] * 0.999
        ba = self.books[symbol].best_ask() or self.assets[symbol] * 1.001
        spread = max(0.01, ba - bb)
        depth = sum(v for _, v in self.books[symbol].depth(3)["bids"]) + sum(v for _, v in self.books[symbol].depth(3)["asks"]) + 1
        lam = 6 + 40 * self.regime.sigma + 8 * self.sentiment.panic + 5 * (spread / max(ba, 1e-9))
        if self._rng.random() < self.regime.burst_prob:
            lam *= 5
        count = self._poisson(lam)

        for _ in range(count):
            side = Side.BUY if self._rng.random() < 0.5 else Side.SELL
            typ_draw = self._rng.random()
            if typ_draw < 0.15 + self.sentiment.panic * 0.25:
                order_type = OrderType.MARKET
            elif typ_draw < 0.9:
                order_type = OrderType.LIMIT
            else:
                self._cancel_random(symbol)
                continue
            size = max(1, int(self._rng.paretovariate(2.0) * (5 if self._rng.random() < 0.95 else 80)))
            impact = self.regime.sigma * math.sqrt(size / depth)
            ref = ba if side == Side.BUY else bb
            aggressive = self._rng.random() < 0.25
            px = None
            if order_type == OrderType.LIMIT:
                offset = spread * (0.2 if aggressive else 1.2)
                px = round(ref + offset if side == Side.BUY else ref - offset, 2)
            self._schedule_order(Order(str(uuid.uuid4()), self._rng.choice(self.agent_ids), symbol, side, size, time.time(), order_type, px), self._rng.uniform(0.001, 0.05))
            self.assets[symbol] = max(0.01, self.assets[symbol] * (1 + (impact if side == Side.BUY else -impact) * 0.04))

    def _schedule_order(self, order: Order, latency: float) -> None:
        execute_at = time.time() + latency
        self.pending_orders[order.order_id] = order
        asyncio.create_task(self.event_q.put((execute_at, Event("new_order", {"order": order}, execute_at))))

    def _handle_order(self, order: Order) -> None:
        self.pending_orders.pop(order.order_id, None)
        trades = self.books[order.symbol].submit(order)
        for tr in trades:
            self.recent_trades[tr.symbol].appendleft(tr)
            self._apply_trade(tr)
            self._broadcast_tick(tr)
            self._log_trade(tr)

    def _apply_trade(self, trade) -> None:
        buyer = self.portfolios[trade.buy_agent]
        seller = self.portfolios[trade.sell_agent]
        buyer.cash -= trade.price * trade.size
        seller.cash += trade.price * trade.size
        buyer.positions[trade.symbol] = buyer.positions.get(trade.symbol, 0) + trade.size
        seller.positions[trade.symbol] = seller.positions.get(trade.symbol, 0) - trade.size
        buyer.history.append({"type": "achat", "symbol": trade.symbol, "qty": trade.size, "price": trade.price})
        seller.history.append({"type": "vente", "symbol": trade.symbol, "qty": trade.size, "price": trade.price})
        if trade.buy_agent == self.player_id or trade.sell_agent == self.player_id:
            self.achievements["premier_trade"] = True
            pnl = self.pnl(self.player_id)
            if pnl > 1000:
                self.achievements["profit_1000"] = True

    def _cancel(self, order_id: str) -> bool:
        order = self.pending_orders.pop(order_id, None)
        if order:
            return True
        for book in self.books.values():
            if book.cancel(order_id):
                return True
        return False

    def _cancel_random(self, symbol: str) -> None:
        for side in ("bids", "asks"):
            levels = getattr(self.books[symbol], side)
            for lvl in levels.values():
                if lvl.orders and self._rng.random() < 0.02:
                    self._cancel(lvl.orders[0].order_id)

    def _cancel_random_from_agent(self, agent_id: str, symbol: str) -> None:
        book = self.books[symbol]
        for levels in (book.bids, book.asks):
            for lvl in levels.values():
                for o in list(lvl.orders)[:2]:
                    if o.agent_id == agent_id and self._rng.random() < 0.5:
                        self._cancel(o.order_id)

    def _poisson(self, lam: float) -> int:
        l = math.exp(-lam)
        k, p = 0, 1.0
        while p > l and k < 1200:
            k += 1
            p *= self._rng.random()
        return max(0, k - 1)

    def market_state(self) -> dict:
        rows = {}
        for s in self.assets:
            bb, ba = self.books[s].best_bid(), self.books[s].best_ask()
            rows[s] = {
                "best_bid": bb,
                "best_ask": ba,
                "mid": (bb + ba) / 2 if bb and ba else None,
                "spread": (ba - bb) if bb and ba else None,
                "regime": self.regime.name,
            }
        return {
            "horaires": "09:30-16:00 (actions), mode crypto 24/7",
            "difficulte": self._difficulty,
            "sentiment": self.sentiment.__dict__,
            "actifs": rows,
            "classement": self.leaderboard(),
            "succes": self.achievements,
        }

    def orderbook_state(self, symbol: str) -> dict:
        return self.books[symbol].depth(10)

    def submit_player_order(self, symbol: str, side: str, size: int, order_type: str, price: float | None) -> str:
        order = Order(str(uuid.uuid4()), self.player_id, symbol, Side(side), size, time.time(), OrderType(order_type), price)
        self._schedule_order(order, 0.001)
        return order.order_id

    def player_portfolio(self) -> dict:
        p = self.portfolios[self.player_id]
        return {
            "solde": p.cash,
            "positions_ouvertes": p.positions,
            "profit_perte": self.pnl(self.player_id),
            "historique": p.history[-40:],
            "ordres_en_attente": [o.order_id for o in self.pending_orders.values() if o.agent_id == self.player_id],
        }

    def pnl(self, agent_id: str) -> float:
        p = self.portfolios[agent_id]
        mark = 0.0
        for symbol, qty in p.positions.items():
            bb = self.books[symbol].best_bid() or self.assets[symbol]
            ba = self.books[symbol].best_ask() or self.assets[symbol]
            mark += qty * ((bb + ba) / 2)
        return p.cash + mark - 100_000.0

    def leaderboard(self) -> list[dict]:
        top = sorted(((aid, self.pnl(aid)) for aid in self.portfolios), key=lambda x: x[1], reverse=True)[:10]
        return [{"trader": t, "pnl": round(v, 2)} for t, v in top]

    def _broadcast_tick(self, trade) -> None:
        payload = {
            "time": trade.timestamp,
            "asset": trade.symbol,
            "price": trade.price,
            "volume": trade.size,
        }
        self._log_price(payload)
        for ws in list(self.ws_clients):
            asyncio.create_task(ws.send_json(payload))

    def _log_trade(self, trade) -> None:
        path = self.log_dir / "trades.csv"
        new = not path.exists()
        with path.open("a", newline="") as f:
            w = csv.writer(f)
            if new:
                w.writerow(["trade_id", "symbol", "price", "size", "buy_agent", "sell_agent", "timestamp"])
            w.writerow([trade.trade_id, trade.symbol, trade.price, trade.size, trade.buy_agent, trade.sell_agent, trade.timestamp])

    def _log_price(self, tick: dict) -> None:
        path = self.log_dir / "prices.csv"
        new = not path.exists()
        with path.open("a", newline="") as f:
            w = csv.writer(f)
            if new:
                w.writerow(["time", "asset", "price", "volume"])
            w.writerow([tick["time"], tick["asset"], tick["price"], tick["volume"]])

        obp = self.log_dir / "orderbook.csv"
        newob = not obp.exists()
        with obp.open("a", newline="") as f:
            w = csv.writer(f)
            if newob:
                w.writerow(["time", "asset", "best_bid", "best_ask", "spread"])
            for asset, book in self.books.items():
                bb = book.best_bid()
                ba = book.best_ask()
                w.writerow([tick["time"], asset, bb, ba, (ba - bb) if bb and ba else None])

        ap = self.log_dir / "agent_activity.csv"
        newa = not ap.exists()
        with ap.open("a", newline="") as f:
            w = csv.writer(f)
            if newa:
                w.writerow(["time", "pending_orders", "agents"])
            w.writerow([tick["time"], len(self.pending_orders), len(self.agent_ids)])
