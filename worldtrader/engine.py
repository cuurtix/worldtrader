from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from typing import Dict, List

from .models import Order, Portfolio, Side, Stock


@dataclass(slots=True)
class Trade:
    symbol: str
    price: float
    quantity: int
    buyer_id: str
    seller_id: str
    tick: int


class MarketEngine:
    def __init__(self, stocks: List[Stock], fee_rate: float = 0.001):
        self.stocks: Dict[str, Stock] = {s.symbol: s for s in stocks}
        self.fee_rate = fee_rate
        self.buy_books: Dict[str, List[Order]] = defaultdict(list)
        self.sell_books: Dict[str, List[Order]] = defaultdict(list)
        self.portfolios: Dict[str, Portfolio] = {}
        self.trades: List[Trade] = []

    def register_trader(self, trader_id: str, portfolio: Portfolio) -> None:
        self.portfolios[trader_id] = portfolio

    def submit_order(self, order: Order, tick: int) -> List[Trade]:
        if order.symbol not in self.stocks:
            return []
        if order.quantity <= 0 or order.limit_price <= 0:
            return []
        if order.trader_id not in self.portfolios:
            return []

        book = self.buy_books if order.side == Side.BUY else self.sell_books
        book[order.symbol].append(order)
        self._sort_book(order.symbol)
        return self._match(order.symbol, tick)

    def _sort_book(self, symbol: str) -> None:
        self.buy_books[symbol].sort(key=lambda o: (-o.limit_price, o.timestamp))
        self.sell_books[symbol].sort(key=lambda o: (o.limit_price, o.timestamp))

    def _match(self, symbol: str, tick: int) -> List[Trade]:
        matched: List[Trade] = []
        buys = self.buy_books[symbol]
        sells = self.sell_books[symbol]

        while buys and sells and buys[0].limit_price >= sells[0].limit_price:
            buy = buys[0]
            sell = sells[0]

            trade_qty = min(buy.quantity, sell.quantity)
            trade_price = (buy.limit_price + sell.limit_price) / 2

            buyer = self.portfolios[buy.trader_id]
            seller = self.portfolios[sell.trader_id]

            total = trade_price * trade_qty
            fee = total * self.fee_rate
            buyer_cost = total + fee
            seller_gain = total - fee

            if not buyer.can_buy(buyer_cost) or not seller.can_sell(symbol, trade_qty):
                break

            buyer.apply_buy(symbol, trade_qty, buyer_cost)
            seller.apply_sell(symbol, trade_qty, seller_gain)

            buy.quantity -= trade_qty
            sell.quantity -= trade_qty

            trade = Trade(
                symbol=symbol,
                price=trade_price,
                quantity=trade_qty,
                buyer_id=buy.trader_id,
                seller_id=sell.trader_id,
                tick=tick,
            )
            matched.append(trade)
            self.trades.append(trade)
            self.stocks[symbol].last_price = trade_price

            if buy.quantity == 0:
                buys.pop(0)
            if sell.quantity == 0:
                sells.pop(0)

        return matched

    def mark_to_market_value(self, trader_id: str) -> float:
        pf = self.portfolios[trader_id]
        value = pf.cash
        for symbol, qty in pf.positions.items():
            value += self.stocks[symbol].last_price * qty
        return value
