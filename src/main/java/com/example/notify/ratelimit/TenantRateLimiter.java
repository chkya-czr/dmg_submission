package com.example.notify.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant in-memory token-bucket rate limiter consulted by the dispatch loop before each claim
 * attempt. Deliberately not persisted (an "Assumption" documented in the README): buckets reset
 * on restart, which is acceptable for a single-instance service and avoids a DB round-trip per
 * claim attempt on the hot path.
 *
 * <p>Takes {@link EffectiveLimits} as a call parameter (rather than depending on
 * {@code TenantLimitPolicyService} itself) purely to avoid a circular bean dependency, since that
 * service calls back into this one to invalidate a tenant's bucket after a limits change.
 */
@Component
public class TenantRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public TenantRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryConsume(String tenantId, EffectiveLimits limits) {
        return bucketFor(tenantId, limits).tryConsume();
    }

    public void refund(String tenantId, EffectiveLimits limits) {
        bucketFor(tenantId, limits).refund();
    }

    /** Drops the cached bucket so the next call rebuilds it from the (now updated) limits. */
    public void invalidate(String tenantId) {
        buckets.remove(tenantId);
    }

    private TokenBucket bucketFor(String tenantId, EffectiveLimits limits) {
        return buckets.computeIfAbsent(tenantId,
                id -> new TokenBucket(limits.rateLimitPerMinute(), limits.burstCapacity(), clock));
    }
}
