package com.example.notify.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Test-only {@link Clock} whose instant can be advanced manually, for deterministic control over
 * time-dependent logic like token-bucket refill. */
class MutableClock extends Clock {

    private Instant instant;

    MutableClock(Instant instant) {
        this.instant = instant;
    }

    void advance(java.time.Duration duration) {
        this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
