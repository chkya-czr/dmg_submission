package com.example.notify.dispatch;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffCalculatorTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void firstAttemptDelayIsWithinHalfToFullOfBaseDelay() {
        BackoffCalculator calculator = new BackoffCalculator(30, 3600, new Random(42));
        Instant next = calculator.computeNextAttemptAt(now, 1);
        long delaySeconds = Duration.between(now, next).getSeconds();
        assertThat(delaySeconds).isBetween(15L, 30L);
    }

    @Test
    void delayGrowsExponentiallyWithAttemptNumber() {
        BackoffCalculator calculator = new BackoffCalculator(30, 3600, new Random(1));
        long attempt1 = Duration.between(now, calculator.computeNextAttemptAt(now, 1)).getSeconds();
        long attempt3 = Duration.between(now, calculator.computeNextAttemptAt(now, 3)).getSeconds();
        // attempt 3 exponential base is 4x attempt 1's (2^2 vs 2^0), so even with independent jitter
        // rolls the attempt-3 floor (50% of 120s = 60s) exceeds the attempt-1 ceiling (100% of 30s).
        assertThat(attempt3).isGreaterThan(attempt1);
    }

    @Test
    void delayIsCappedAtMaxDelaySeconds() {
        BackoffCalculator calculator = new BackoffCalculator(30, 100, new Random(7));
        Instant next = calculator.computeNextAttemptAt(now, 10); // 30 * 2^9 would be huge uncapped
        long delaySeconds = Duration.between(now, next).getSeconds();
        assertThat(delaySeconds).isBetween(50L, 100L);
    }

    @Test
    void jitterKeepsDelayWithinExpectedBounds() {
        BackoffCalculator calculator = new BackoffCalculator(60, 3600, new Random(123));
        for (int i = 0; i < 50; i++) {
            Instant next = calculator.computeNextAttemptAt(now, 2); // exponential = 120s
            long delaySeconds = Duration.between(now, next).getSeconds();
            assertThat(delaySeconds).isBetween(60L, 120L);
        }
    }
}
