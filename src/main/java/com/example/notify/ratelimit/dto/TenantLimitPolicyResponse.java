package com.example.notify.ratelimit.dto;

import com.example.notify.ratelimit.EffectiveLimits;
import com.example.notify.ratelimit.TenantLimitPolicy;

public record TenantLimitPolicyResponse(
        String tenantId,
        Integer rateLimitPerMinuteOverride,
        Integer burstCapacityOverride,
        Integer maxRetryAttemptsOverride,
        int effectiveRateLimitPerMinute,
        int effectiveBurstCapacity,
        int effectiveMaxRetryAttempts
) {

    public static TenantLimitPolicyResponse of(String tenantId, TenantLimitPolicy policy, EffectiveLimits effective) {
        return new TenantLimitPolicyResponse(
                tenantId,
                policy == null ? null : policy.getRateLimitPerMinute(),
                policy == null ? null : policy.getBurstCapacity(),
                policy == null ? null : policy.getMaxRetryAttempts(),
                effective.rateLimitPerMinute(),
                effective.burstCapacity(),
                effective.maxRetryAttempts());
    }
}
