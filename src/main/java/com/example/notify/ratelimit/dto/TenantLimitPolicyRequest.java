package com.example.notify.ratelimit.dto;

import jakarta.validation.constraints.Min;

/** All fields optional/nullable: a null field means "fall back to the global default". */
public record TenantLimitPolicyRequest(
        @Min(1) Integer rateLimitPerMinute,
        @Min(1) Integer burstCapacity,
        @Min(0) Integer maxRetryAttempts
) {
}
