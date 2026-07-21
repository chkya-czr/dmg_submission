package com.example.notify.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final TenantRateLimiter limiter = new TenantRateLimiter(clock);
    private final EffectiveLimits limits = new EffectiveLimits(60, 5, 5); // 1 token/sec, burst of 5

    @Test
    void allowsConsumptionUpToBurstCapacityThenBlocks() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume("tenant-a", limits)).as("consume #" + i).isTrue();
        }
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();
    }

    @Test
    void refillsOverTimeAtTheConfiguredRate() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("tenant-a", limits);
        }
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();

        clock.advance(Duration.ofSeconds(3)); // 1 token/sec -> ~3 tokens back

        assertThat(limiter.tryConsume("tenant-a", limits)).isTrue();
        assertThat(limiter.tryConsume("tenant-a", limits)).isTrue();
        assertThat(limiter.tryConsume("tenant-a", limits)).isTrue();
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();
    }

    @Test
    void refundGivesBackAConsumedToken() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("tenant-a", limits);
        }
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();

        limiter.refund("tenant-a", limits);

        assertThat(limiter.tryConsume("tenant-a", limits)).isTrue();
    }

    @Test
    void tenantsHaveIndependentBuckets() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("tenant-a", limits);
        }
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();
        assertThat(limiter.tryConsume("tenant-b", limits)).isTrue();
    }

    @Test
    void invalidateRebuildsTheBucketFromFreshLimits() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("tenant-a", limits);
        }
        assertThat(limiter.tryConsume("tenant-a", limits)).isFalse();

        limiter.invalidate("tenant-a");

        EffectiveLimits biggerBurst = new EffectiveLimits(60, 10, 5);
        assertThat(limiter.tryConsume("tenant-a", biggerBurst)).isTrue();
    }
}
