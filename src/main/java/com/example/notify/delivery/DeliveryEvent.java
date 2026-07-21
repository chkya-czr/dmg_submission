package com.example.notify.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Immutable audit-trail row for one delivery state transition. */
@Entity
@Table(name = "delivery_event")
public class DeliveryEvent {

    @Id
    private String id;

    @Column(name = "delivery_id", nullable = false)
    private String deliveryId;

    @Column(name = "attempt_number")
    private Integer attemptNumber;

    @Column(name = "attempt_token")
    private String attemptToken;

    @Column(name = "from_status", nullable = false)
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeliveryEvent() {
        // JPA
    }

    public DeliveryEvent(String deliveryId, Integer attemptNumber, String attemptToken, DeliveryStatus fromStatus,
                          DeliveryStatus toStatus, String errorMessage, String workerId) {
        this.id = UUID.randomUUID().toString();
        this.deliveryId = deliveryId;
        this.attemptNumber = attemptNumber;
        this.attemptToken = attemptToken;
        this.fromStatus = fromStatus.name();
        this.toStatus = toStatus.name();
        this.errorMessage = errorMessage;
        this.workerId = workerId;
        this.createdAt = Instant.now();
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
