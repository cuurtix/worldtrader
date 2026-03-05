from __future__ import annotations

import asyncio
import logging

import uvicorn


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
    loop = asyncio.new_event_loop()
    loop.set_debug(True)
    asyncio.set_event_loop(loop)
    uvicorn.run("worldtrader.realtime.server:app", host="0.0.0.0", port=8000, reload=False)
