from __future__ import annotations

from contextlib import asynccontextmanager
import asyncio

from fastapi import FastAPI, WebSocket
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

from .engine import RealTimeMarket
from .ui import INDEX_HTML

market = RealTimeMarket(tps=20)


class OrderReq(BaseModel):
    asset: str
    side: str
    taille: int
    type_ordre: str = "MARKET"
    prix_limite: float | None = None


class CancelReq(BaseModel):
    order_id: str


@asynccontextmanager
async def lifespan(app: FastAPI):
    await market.start()
    yield
    market.running = False


app = FastAPI(title="WorldTrader Temps Réel", lifespan=lifespan)


@app.get("/", response_class=HTMLResponse)
async def home() -> str:
    return INDEX_HTML


@app.get("/marche")
async def marche() -> dict:
    return market.market_state()


@app.get("/orderbook")
async def orderbook(asset: str = "AAPL") -> dict:
    return market.orderbook_state(asset)


@app.post("/ordre")
async def ordre(req: OrderReq) -> dict:
    oid = market.submit_player_order(req.asset, req.side, req.taille, req.type_ordre, req.prix_limite)
    return {"status": "ok", "order_id": oid, "message": "Ordre enregistré"}


@app.post("/annuler")
async def annuler(req: CancelReq) -> dict:
    ok = market._cancel(req.order_id)
    return {"status": "ok" if ok else "ko", "message": "Ordre annulé" if ok else "Ordre introuvable"}


@app.get("/portefeuille")
async def portefeuille() -> dict:
    return market.player_portfolio()


@app.websocket("/ws/ticks")
async def ws_ticks(ws: WebSocket) -> None:
    await ws.accept()
    market.ws_clients.add(ws)
    try:
        while True:
            await asyncio.sleep(30)
    except Exception:
        pass
    finally:
        market.ws_clients.discard(ws)
