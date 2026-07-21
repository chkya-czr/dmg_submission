package com.example.notify.notification;

import com.example.notify.common.persistence.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A logical "send" submitted by a tenant admin. Deliveries (one row per recipient x channel) are
 * created later by {@code RequestExpansionScheduler} once the request is due - not synchronously
 * on create - so scheduled sends and immediate sends share the same expansion path.
 */
@Entity
@Table(name = "notification_request")
public class NotificationRequest {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "template_code", nullable = false)
    private String templateCode;

    @Convert(converter = StringMapJsonConverter.class)
    @Column(name = "variables_json", nullable = false)
    private Map<String, String> variables;

    @Convert(converter = RecipientSpecListConverter.class)
    @Column(name = "recipients_json", nullable = false)
    private List<RecipientSpec> recipients;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationRequestStatus status;

    /** Plain optimistic-concurrency counter, manually managed via conditional UPDATEs in
     * {@code NotificationRequestRepository} - intentionally not a JPA {@code @Version} column,
     * so bulk claim queries fully control it. */
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationRequest() {
        // JPA
    }

    public NotificationRequest(String tenantId, String templateCode, Map<String, String> variables,
                                List<RecipientSpec> recipients, Instant scheduledAt, String idempotencyKey,
                                String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.templateCode = templateCode;
        this.variables = variables;
        this.recipients = recipients;
        this.scheduledAt = scheduledAt;
        this.idempotencyKey = idempotencyKey;
        this.status = scheduledAt != null ? NotificationRequestStatus.SCHEDULED : NotificationRequestStatus.PENDING;
        this.version = 0;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public List<RecipientSpec> getRecipients() {
        return recipients;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public NotificationRequestStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
