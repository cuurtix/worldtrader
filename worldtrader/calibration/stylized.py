from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Dict, List


@dataclass
class StylizedFactsReport:
    kurtosis: float
    skewness: float
    acf_abs_1: float


def _moments(xs: List[float]) -> tuple[float, float, float]:
    n = len(xs)
    if n == 0:
        return 0.0, 0.0, 0.0
    mu = sum(xs) / n
    centered = [x - mu for x in xs]
    var = sum(c * c for c in centered) / max(1, n)
    std = math.sqrt(max(var, 1e-12))
    skew = sum((c / std) ** 3 for c in centered) / max(1, n)
    kurt = sum((c / std) ** 4 for c in centered) / max(1, n)
    return mu, skew, kurt


def analyze_series(xs: List[float]) -> StylizedFactsReport:
    _, skew, kurt = _moments(xs)
    if len(xs) < 3:
        return StylizedFactsReport(kurtosis=kurt, skewness=skew, acf_abs_1=0.0)
    absx = [abs(x) for x in xs]
    mu = sum(absx) / len(absx)
    num = sum((absx[i] - mu) * (absx[i - 1] - mu) for i in range(1, len(absx)))
    den = sum((v - mu) ** 2 for v in absx)
    acf = num / den if den else 0.0
    return StylizedFactsReport(kurtosis=kurt, skewness=skew, acf_abs_1=acf)


def analyze_all(data: Dict[str, List[float]]) -> Dict[str, Dict[str, float]]:
    out: Dict[str, Dict[str, float]] = {}
    for symbol, series in data.items():
        rep = analyze_series(series)
        out[symbol] = {"kurtosis": rep.kurtosis, "skewness": rep.skewness, "acf_abs_1": rep.acf_abs_1}
    return out
