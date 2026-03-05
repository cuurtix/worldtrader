from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict


def load_config(path: str) -> Dict[str, Any]:
    raw = Path(path).read_text(encoding="utf-8")
    if path.endswith(".json"):
        return json.loads(raw)
    try:
        import yaml  # type: ignore
        return yaml.safe_load(raw)
    except Exception:
        return json.loads(raw)
