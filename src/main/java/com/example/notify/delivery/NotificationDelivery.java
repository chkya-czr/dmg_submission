package com.example.notify.delivery;

import com.example.notify.common.model.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One row per (recipient, channel) - the unit of work the dispatch loop claims and sends. Retries
 * reuse this same row (attempt_count increments in place); a new row is never created for a retry,
 * which is one of the mechanisms preventing duplicate deliveries.
 */
@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {

    @Id
    private String id;

    @Column(name = "notification_request_id", nullable = false)
    private String notificationRequestId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "recipient_address", nullable = false)
    private String recipientAddress;

    @Column(name = "rendered_subject")
    private String renderedSubject;

    @Column(name = "rendered_body", nullable = false)
    private String renderedBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "sent_at")
    private Instant sentAt;

    /** Plain optimistic-concurrency counter, manually managed via conditional UPDATEs in
     * {@code NotificationDeliveryRepository} (claim/complete/sweep) - not a JPA {@code @Version}
     * column, so bulk queries fully control it. */
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationDelivery() {
        // JPA
    }

    public NotificationDelivery(String notificationRequestId, String tenantId, Channel channel, String recipientId,
                                 String recipientAddress, String renderedSubject, String renderedBody,
                                 DeliveryStatus initialStatus, int maxAttempts, Instant nextAttemptAt) {
        this.id = UUID.randomUUID().toString();
        this.notificationRequestId = notificationRequestId;
        this.tenantId = tenantId;
        this.channel = channel;
        this.recipientId = recipientId;
        this.recipientAddress = recipientAddress;
        this.renderedSubject = renderedSubject;
        this.renderedBody = renderedBody;
        this.status = initialStatus;
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.version = 0;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getNotificationRequestId() {
        return notificationRequestId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public String getRenderedBody() {
        return renderedBody;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
