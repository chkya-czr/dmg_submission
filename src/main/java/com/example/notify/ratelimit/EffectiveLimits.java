package com.example.notify.ratelimit;

/** The resolved (override-or-default) limits actually in effect for a tenant. */
public record EffectiveLimits(int rateLimitPerMinute, int burstCapacity, int maxRetryAttempts) {
}
