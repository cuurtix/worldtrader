from __future__ import annotations

import random
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List

from worldtrader.agents.base import Agent, build_agents
from worldtrader.calibration.stylized import analyze_all
from worldtrader.contagion.network import ContagionNetwork
from worldtrader.core.time import default_clock
from worldtrader.correlation.model import CorrelationModel
from worldtrader.events.engine import EventEngine
from worldtrader.instruments.universe import InstrumentUniverse
from worldtrader.macro.engine import MacroEngine
from worldtrader.metrics.collector import MetricsCollector
from worldtrader.microstructure.models import ImpactModel, SpreadModel
from worldtrader.orderbook.lob import MatchingEngine
from worldtrader.replay.store import ReplayStore
from worldtrader.risk.engine import RiskManager
from worldtrader.types import MacroState, RegimeState, Venue
from worldtrader.venues.venue_engine import VenueEngine


@dataclass
class SimulationRunner:
    config: Dict[str, object]

    def run(self, ticks: int, seed: int) -> Dict[str, object]:
        rng = random.Random(seed)
        universe = InstrumentUniverse.from_config(self.config)
        venues = VenueEngine({v["name"]: Venue(**v) for v in self.config["venues"]})
        agents = build_agents(self.config, universe.instruments)
        matcher = MatchingEngine()
        macro = MacroEngine(MacroState(0.02, 0.02, 0.03, 1.0, 0.0))
        event_engine = EventEngine(self.config.get("event_intensity", 0.03))
        corr = CorrelationModel(self.config.get("base_corr", 0.2))
        contagion = ContagionNetwork(self.config.get("network_exposures", {}))
        risk = RiskManager(self.config.get("maintenance_margin", 0.25))
        impact = ImpactModel(**self.config.get("impact", {}))
        spread_model = SpreadModel(self.config.get("base_spread_bps", 5.0))
        metrics = MetricsCollector()
        replay = ReplayStore()
        clock = default_clock()
        returns: Dict[str, float] = {s: 0.0 for s in universe.iter_symbols()}
        trades_log: List[dict] = []

        regime = RegimeState("range", stress=0.1, corr_multiplier=1.0)

        for _ in range(ticks):
            now = clock.advance()
            seasonality = clock.intraday_seasonality()
            macro_state = macro.update(rng)
            events = event_engine.emit(clock.tick, macro_state, rng)
            for ev in events:
                metrics.collect_event(ev.to_dict())

            stress_boost = sum(ev.payload.get("shock", 0.0) + abs(ev.payload.get("surprise", 0.0)) for ev in events)
            regime.stress = min(2.0, max(0.0, 0.7 * regime.stress + 0.15 * stress_boost + max(0.0, -macro_state.risk_appetite * 0.05)))
            regime.name = "crash" if regime.stress > 1.2 else "trend" if abs(macro_state.risk_appetite) > 0.8 else "range"

            corr_matrix = corr.matrix(list(universe.iter_symbols()), stress=regime.stress)

            context = {
                "instruments": universe.instruments,
                "returns": returns,
                "macro": macro_state,
                "regime": regime,
                "corr": corr_matrix,
                "seasonality": seasonality,
            }

            all_orders = []
            for agent in agents:
                all_orders.extend(agent.decide(clock.tick, context, rng))
            for order in all_orders:
                venues.schedule(order, clock.tick)

            live_orders = venues.release(clock.tick)
            tick_trades = []
            for order in live_orders:
                tick_trades.extend(matcher.submit(order, clock.tick))

            signed_flow: Dict[str, float] = {s: 0.0 for s in universe.iter_symbols()}
            for tr in tick_trades:
                trades_log.append(tr.to_dict())
                signed_flow[tr.instrument] += tr.qty
                inst = universe.instruments[tr.instrument]
                prev = inst.mid
                inst.last = tr.price
                inst.mid = max(inst.tick_size, tr.price)
                returns[tr.instrument] = (inst.mid - prev) / prev if prev else 0.0
                metrics.collect_return(tr.instrument, returns[tr.instrument])

            for symbol, inst in universe.instruments.items():
                temp = impact.temporary_impact(signed_flow[symbol], max(0.1, inst.liquidity_state * 100))
                perm = impact.permanent_impact(signed_flow[symbol], max(0.1, inst.liquidity_state * 100))
                inst.mid = max(inst.tick_size, inst.mid * (1 + temp + perm))
                inst.volatility_state = min(0.3, 0.94 * inst.volatility_state + 0.06 * abs(returns[symbol]) + 0.02 * regime.stress)
                if inst.asset_class.value in ("equity", "index") and returns[symbol] < 0:
                    inst.volatility_state *= 1.05
                spread = spread_model.compute(inst.volatility_state, signed_flow[symbol] / 100.0, inst.liquidity_state, regime.stress)
                inst.bid = max(inst.tick_size, inst.mid * (1 - spread / 2))
                inst.ask = inst.mid * (1 + spread / 2)
                inst.liquidity_state = max(0.1, min(2.0, inst.liquidity_state + 0.03 * macro_state.liquidity - 0.04 * regime.stress))

            losses = {}
            for agent in agents:
                risk.update_equity(agent, universe.instruments)
                liq_orders = risk.liquidation_orders(agent, universe.instruments, clock.tick)
                losses[agent.agent_id] = max(0.0, -agent.portfolio.margin.equity)
                for lo in liq_orders:
                    tick_trades.extend(matcher.submit(lo, clock.tick))
            _ = contagion.propagate(losses)
            metrics.collect_stress(regime.stress)

            replay.save_snapshot(clock.tick, {
                "time": now.isoformat(),
                "regime": regime.name,
                "stress": regime.stress,
                "bbo": {s: matcher.bbo(s) for s in universe.iter_symbols()},
            })

        summary = analyze_all(metrics.returns)
        return {
            "trades": trades_log,
            "snapshots": replay.snapshots,
            "metrics": metrics,
            "summary": summary,
        }

    def export(self, result: Dict[str, object], run_dir: Path) -> None:
        result["metrics"].export(run_dir, result["trades"], result["snapshots"])
