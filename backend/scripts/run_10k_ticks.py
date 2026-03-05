#!/usr/bin/env python3
import json
import time
import urllib.request

BASE = "http://localhost:8000"


def get(path: str):
    with urllib.request.urlopen(BASE + path, timeout=10) as r:
        return json.loads(r.read().decode())


def post(path: str):
    req = urllib.request.Request(BASE + path, data=b"", method="POST")
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read().decode())


def main():
    status = get("/api/v1/market")
    interval = int(status.get("intervalMillis", 300))
    print("Initial market status:", status)

    ticks_target = status.get("tickCount", 0) + 10_000
    print("Running until tick", ticks_target)

    while True:
        s = get("/api/v1/market")
        if s.get("tickCount", 0) >= ticks_target:
            break
        time.sleep(max(0.01, interval / 1000.0 * 0.5))

    stats = get("/api/v1/metrics/market")
    print("\n=== Market microstructure stats ===")
    print(json.dumps(stats, indent=2))


if __name__ == "__main__":
    main()
