package com.example.notify.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Singleton row (fixed id) holding the platform-wide defaults every tenant falls back to when
 * it has no {@code tenant_limit_policy} override. */
@Entity
@Table(name = "global_setting")
public class GlobalSetting {

    public static final String SINGLETON_ID = "00000000-0000-0000-0000-000000000001";

    @Id
    private String id;

    @Column(name = "default_rate_limit_per_minute", nullable = false)
    private int defaultRateLimitPerMinute;

    @Column(name = "default_burst_capacity", nullable = false)
    private int defaultBurstCapacity;

    @Column(name = "default_max_retry_attempts", nullable = false)
    private int defaultMaxRetryAttempts;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GlobalSetting() {
        // JPA
    }

    public void update(int rateLimitPerMinute, int burstCapacity, int maxRetryAttempts) {
        this.defaultRateLimitPerMinute = rateLimitPerMinute;
        this.defaultBurstCapacity = burstCapacity;
        this.defaultMaxRetryAttempts = maxRetryAttempts;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public int getDefaultRateLimitPerMinute() {
        return defaultRateLimitPerMinute;
    }

    public int getDefaultBurstCapacity() {
        return defaultBurstCapacity;
    }

    public int getDefaultMaxRetryAttempts() {
        return defaultMaxRetryAttempts;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
