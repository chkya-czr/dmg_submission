package com.example.notify.ratelimit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Per-tenant overrides for rate limit / retry-attempt defaults. Any column left {@code null}
 * falls back to {@link com.example.notify.settings.GlobalSetting}. */
@Entity
@Table(name = "tenant_limit_policy")
public class TenantLimitPolicy {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @Column(name = "burst_capacity")
    private Integer burstCapacity;

    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TenantLimitPolicy() {
        // JPA
    }

    public TenantLimitPolicy(String tenantId) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(Integer rateLimitPerMinute, Integer burstCapacity, Integer maxRetryAttempts) {
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.burstCapacity = burstCapacity;
        this.maxRetryAttempts = maxRetryAttempts;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public Integer getBurstCapacity() {
        return burstCapacity;
    }

    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
