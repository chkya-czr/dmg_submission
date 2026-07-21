package com.example.notify.channel;

import com.example.notify.common.model.Channel;
import com.example.notify.common.persistence.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Per-tenant, per-channel configuration. {@code config} holds sender-specific settings - for the
 * simulated senders, an optional "failureRate" (0..1) used to exercise retry/backoff in tests. */
@Entity
@Table(name = "channel_config")
public class ChannelConfig {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private boolean enabled = true;

    @Convert(converter = StringMapJsonConverter.class)
    @Column(name = "config_json", nullable = false)
    private Map<String, String> config;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChannelConfig() {
        // JPA
    }

    public ChannelConfig(String tenantId, Channel channel, boolean enabled, Map<String, String> config) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.channel = channel;
        this.enabled = enabled;
        this.config = config;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(boolean enabled, Map<String, String> config) {
        this.enabled = enabled;
        this.config = config;
        this.updatedAt = Instant.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
