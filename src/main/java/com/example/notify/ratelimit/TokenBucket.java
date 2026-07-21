package com.example.notify.ratelimit;

import java.time.Clock;
import java.time.Instant;

/**
 * A simple thread-safe token bucket. Refill is computed lazily from elapsed wall-clock time on
 * every call (no background thread needed): {@code tokens += elapsedSeconds * ratePerSecond},
 * capped at {@code capacity}. {@link Clock} is injected so tests can control time deterministically.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;

    private double tokens;
    private Instant lastRefill;

    public TokenBucket(int ratePerMinute, int capacity, Clock clock) {
        this.capacity = capacity;
        this.refillPerSecond = ratePerMinute / 60.0;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.instant();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    /** Returns a token that was consumed but not actually used (e.g. the claim lost a race). */
    public synchronized void refund() {
        tokens = Math.min(capacity, tokens + 1.0);
    }

    private void refill() {
        Instant now = clock.instant();
        double elapsedSeconds = Math.max(0, java.time.Duration.between(lastRefill, now).toNanos() / 1_000_000_000.0);
        if (elapsedSeconds > 0) {
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefill = now;
        }
    }

    public synchronized double availableTokens() {
        refill();
        return tokens;
    }
}
