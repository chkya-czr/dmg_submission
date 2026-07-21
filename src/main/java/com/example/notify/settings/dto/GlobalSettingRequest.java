package com.example.notify.settings.dto;

import jakarta.validation.constraints.Min;

public record GlobalSettingRequest(
        @Min(1) int rateLimitPerMinute,
        @Min(1) int burstCapacity,
        @Min(0) int maxRetryAttempts
) {
}
