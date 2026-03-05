from __future__ import annotations

import heapq
import itertools
from collections import defaultdict, deque
from dataclasses import dataclass
from typing import Deque

from .models import Order, OrderType, Side, Trade


@dataclass
class _QueueEntry:
    price: float
    orders: Deque[Order]


class LimitOrderBook:
    def __init__(self, symbol: str) -> None:
        self.symbol = symbol
        self.bids: dict[float, _QueueEntry] = {}
        self.asks: dict[float, _QueueEntry] = {}
        self._bid_heap: list[float] = []
        self._ask_heap: list[float] = []
        self._trade_counter = itertools.count(1)
        self._order_index: dict[str, tuple[Side, float]] = {}

    def best_bid(self) -> float | None:
        while self._bid_heap and -self._bid_heap[0] not in self.bids:
            heapq.heappop(self._bid_heap)
        return -self._bid_heap[0] if self._bid_heap else None

    def best_ask(self) -> float | None:
        while self._ask_heap and self._ask_heap[0] not in self.asks:
            heapq.heappop(self._ask_heap)
        return self._ask_heap[0] if self._ask_heap else None

    def submit(self, order: Order) -> list[Trade]:
        if order.size <= 0:
            return []
        trades: list[Trade] = []
        remaining = order.size
        while remaining > 0:
            bb, ba = self.best_bid(), self.best_ask()
            cross = False
            if order.side == Side.BUY and ba is not None:
                cross = order.order_type == OrderType.MARKET or (order.price is not None and order.price >= ba)
            if order.side == Side.SELL and bb is not None:
                cross = order.order_type == OrderType.MARKET or (order.price is not None and order.price <= bb)
            if not cross:
                break

            if order.side == Side.BUY:
                level_price = ba
                queue = self.asks[level_price].orders
                maker = queue[0]
                qty = min(remaining, maker.size)
                maker.size -= qty
                remaining -= qty
                trades.append(Trade(f"T{next(self._trade_counter)}", order.symbol, level_price, qty, order.agent_id, maker.agent_id, order.timestamp))
                if maker.size == 0:
                    queue.popleft()
                    self._order_index.pop(maker.order_id, None)
                if not queue:
                    del self.asks[level_price]
            else:
                level_price = bb
                queue = self.bids[level_price].orders
                maker = queue[0]
                qty = min(remaining, maker.size)
                maker.size -= qty
                remaining -= qty
                trades.append(Trade(f"T{next(self._trade_counter)}", order.symbol, level_price, qty, maker.agent_id, order.agent_id, order.timestamp))
                if maker.size == 0:
                    queue.popleft()
                    self._order_index.pop(maker.order_id, None)
                if not queue:
                    del self.bids[level_price]

        if remaining > 0 and order.order_type == OrderType.LIMIT and order.price is not None:
            rest = Order(**{**order.__dict__, "size": remaining})
            self._add_resting(rest)
        return trades

    def _add_resting(self, order: Order) -> None:
        levels = self.bids if order.side == Side.BUY else self.asks
        heap = self._bid_heap if order.side == Side.BUY else self._ask_heap
        price_key = -order.price if order.side == Side.BUY else order.price
        if order.price not in levels:
            levels[order.price] = _QueueEntry(order.price, deque())
            heapq.heappush(heap, price_key)
        levels[order.price].orders.append(order)
        self._order_index[order.order_id] = (order.side, order.price)

    def cancel(self, order_id: str) -> bool:
        meta = self._order_index.get(order_id)
        if not meta:
            return False
        side, price = meta
        levels = self.bids if side == Side.BUY else self.asks
        level = levels.get(price)
        if not level:
            return False
        for idx, order in enumerate(level.orders):
            if order.order_id == order_id:
                del level.orders[idx]
                self._order_index.pop(order_id, None)
                if not level.orders:
                    del levels[price]
                return True
        return False

    def depth(self, levels: int = 5) -> dict:
        bid_prices = sorted(self.bids.keys(), reverse=True)[:levels]
        ask_prices = sorted(self.asks.keys())[:levels]
        return {
            "bids": [(p, sum(o.size for o in self.bids[p].orders)) for p in bid_prices],
            "asks": [(p, sum(o.size for o in self.asks[p].orders)) for p in ask_prices],
        }

    def queue_rank(self, order_id: str) -> int | None:
        meta = self._order_index.get(order_id)
        if not meta:
            return None
        side, price = meta
        level = (self.bids if side == Side.BUY else self.asks).get(price)
        if not level:
            return None
        for idx, order in enumerate(level.orders):
            if order.order_id == order_id:
                return idx + 1
        return None
