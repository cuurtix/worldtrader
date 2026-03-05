package com.worldtrader.api.market.secure.service;

import com.worldtrader.api.market.secure.error.InvalidOrderError;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private record Window(int count, long startEpochSec) {}
    private final Map<String, Window> userOrderWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();

    public void check(String userId, String ip) {
        limit(userOrderWindows, "u:" + userId, 10);
        limit(ipWindows, "ip:" + ip, 100);
    }

    private void limit(Map<String, Window> map, String key, int max) {
        long now = Instant.now().getEpochSecond();
        Window current = map.get(key);
        if (current == null || now - current.startEpochSec() >= 60) {
            map.put(key, new Window(1, now));
            return;
        }
        if (current.count() >= max) throw new InvalidOrderError("Rate limit exceeded");
        map.put(key, new Window(current.count() + 1, current.startEpochSec()));
    }
}
