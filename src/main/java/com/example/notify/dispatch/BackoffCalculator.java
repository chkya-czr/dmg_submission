package com.example.notify.dispatch;

import java.time.Instant;
import java.util.Random;

/**
 * Exponential backoff with jitter: {@code delay = exponential * [50%, 100%]}, where
 * {@code exponential = min(maxDelaySeconds, baseDelaySeconds * 2^(attemptNumber-1))}. Framework-free
 * and takes {@link Random} via the constructor so it's trivially unit-testable with a seeded value.
 */
public class BackoffCalculator {

    private final int baseDelaySeconds;
    private final int maxDelaySeconds;
    private final Random random;

    public BackoffCalculator(int baseDelaySeconds, int maxDelaySeconds, Random random) {
        this.baseDelaySeconds = baseDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.random = random;
    }

    public Instant computeNextAttemptAt(Instant now, int attemptNumber) {
        double exponential = Math.min(maxDelaySeconds, baseDelaySeconds * Math.pow(2, attemptNumber - 1));
        double delaySeconds = exponential * 0.5 + random.nextDouble() * exponential * 0.5;
        return now.plusMillis(Math.round(delaySeconds * 1000));
    }
}
