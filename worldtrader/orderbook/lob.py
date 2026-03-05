from __future__ import annotations

import itertools
from collections import defaultdict, deque
from dataclasses import dataclass, field
from typing import Deque, Dict, List, Tuple

from worldtrader.types import Order, OrderType, Side, Trade


@dataclass
class OrderBookLevel:
    price: float
    queue: Deque[Order] = field(default_factory=deque)


@dataclass
class OrderBook:
    symbol: str
    bids: Dict[float, OrderBookLevel] = field(default_factory=dict)
    asks: Dict[float, OrderBookLevel] = field(default_factory=dict)


class MatchingEngine:
    def __init__(self) -> None:
        self.books: Dict[str, OrderBook] = {}
        self._trade_counter = itertools.count(1)

    def _book(self, symbol: str) -> OrderBook:
        if symbol not in self.books:
            self.books[symbol] = OrderBook(symbol=symbol)
        return self.books[symbol]

    def submit(self, order: Order, tick: int) -> List[Trade]:
        order.validate()
        book = self._book(order.instrument)
        trades = self._match(order, book, tick)
        residual_qty = order.qty - sum(t.qty for t in trades if t.buyer == order.agent_id or t.seller == order.agent_id)

        if residual_qty > 0 and order.order_type in (OrderType.LIMIT, OrderType.STOP_LIMIT) and order.tif.value == "gtc":
            rest = Order(**{**order.__dict__, "qty": residual_qty})
            levels = book.bids if order.side == Side.BUY else book.asks
            level = levels.setdefault(order.limit_price, OrderBookLevel(price=order.limit_price))
            level.queue.append(rest)
        return trades

    def _best_levels(self, book: OrderBook) -> Tuple[List[float], List[float]]:
        return sorted(book.bids.keys(), reverse=True), sorted(book.asks.keys())

    def _match(self, incoming: Order, book: OrderBook, tick: int) -> List[Trade]:
        trades: List[Trade] = []
        incoming_rem = incoming.qty
        while incoming_rem > 0:
            bids, asks = self._best_levels(book)
            if incoming.side == Side.BUY:
                if not asks:
                    break
                best_ask = asks[0]
                if incoming.order_type != OrderType.MARKET and incoming.limit_price is not None and incoming.limit_price < best_ask:
                    break
                level = book.asks[best_ask]
                maker = level.queue[0]
                qty = min(incoming_rem, maker.qty)
                price = best_ask
                maker.qty -= qty
                incoming_rem -= qty
                trades.append(Trade(f"T{next(self._trade_counter)}", incoming.instrument, incoming.venue, price, qty, incoming.agent_id, maker.agent_id, tick))
                if maker.qty == 0:
                    level.queue.popleft()
                    if not level.queue:
                        del book.asks[best_ask]
            else:
                if not bids:
                    break
                best_bid = bids[0]
                if incoming.order_type != OrderType.MARKET and incoming.limit_price is not None and incoming.limit_price > best_bid:
                    break
                level = book.bids[best_bid]
                maker = level.queue[0]
                qty = min(incoming_rem, maker.qty)
                price = best_bid
                maker.qty -= qty
                incoming_rem -= qty
                trades.append(Trade(f"T{next(self._trade_counter)}", incoming.instrument, incoming.venue, price, qty, maker.agent_id, incoming.agent_id, tick))
                if maker.qty == 0:
                    level.queue.popleft()
                    if not level.queue:
                        del book.bids[best_bid]
        return trades

    def bbo(self, symbol: str) -> Tuple[float | None, float | None]:
        book = self._book(symbol)
        bids, asks = self._best_levels(book)
        return (bids[0] if bids else None, asks[0] if asks else None)

    def depth_snapshot(self, symbol: str, levels: int = 5) -> Dict[str, List[Tuple[float, int]]]:
        book = self._book(symbol)
        bid_levels = sorted(book.bids.keys(), reverse=True)[:levels]
        ask_levels = sorted(book.asks.keys())[:levels]
        return {
            "bids": [(p, sum(o.qty for o in book.bids[p].queue)) for p in bid_levels],
            "asks": [(p, sum(o.qty for o in book.asks[p].queue)) for p in ask_levels],
        }
