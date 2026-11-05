package com.pethop.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter {
    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter() { this(Clock.systemUTC()); }
    public InMemoryRateLimiter(Clock clock) { this.clock = clock; }

    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        if (maxRequests <= 0 || windowSeconds <= 0) return false;
        long now = clock.millis();
        long windowMillis = Math.multiplyExact(windowSeconds, 1000L);
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) return new Bucket(now, 1);
            current.count++;
            return current;
        });
        if (buckets.size() > 1024) buckets.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        return bucket.count <= maxRequests;
    }

    private static final class Bucket {
        private final long startedAt;
        private int count;
        private Bucket(long startedAt, int count) { this.startedAt = startedAt; this.count = count; }
    }
}
