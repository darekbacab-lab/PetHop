package com.pethop.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {
    @Test
    void blocksAfterLimitAndResetsAtWindowBoundary() {
        MutableClock clock = new MutableClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
        assertThat(limiter.tryAcquire("login:ip", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("login:ip", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("login:ip", 2, 60)).isFalse();
        assertThat(limiter.tryAcquire("other:ip", 2, 60)).isTrue();
        clock.advance(Duration.ofSeconds(60));
        assertThat(limiter.tryAcquire("login:ip", 2, 60)).isTrue();
    }

    @Test
    void rejectsInvalidPolicies() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();
        assertThat(limiter.tryAcquire("key", 0, 60)).isFalse();
        assertThat(limiter.tryAcquire("key", 1, 0)).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.EPOCH;
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
