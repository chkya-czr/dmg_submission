package com.example.notify.settings.dto;

import com.example.notify.settings.GlobalSetting;

import java.time.Instant;

public record GlobalSettingResponse(int rateLimitPerMinute, int burstCapacity, int maxRetryAttempts, Instant updatedAt) {

    public static GlobalSettingResponse from(GlobalSetting setting) {
        return new GlobalSettingResponse(setting.getDefaultRateLimitPerMinute(), setting.getDefaultBurstCapacity(),
                setting.getDefaultMaxRetryAttempts(), setting.getUpdatedAt());
    }
}
