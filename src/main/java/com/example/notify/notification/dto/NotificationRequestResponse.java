package com.example.notify.notification.dto;

import com.example.notify.notification.NotificationRequest;
import com.example.notify.notification.NotificationRequestStatus;

import java.time.Instant;
import java.util.Map;

public record NotificationRequestResponse(
        String id, String tenantId, String templateCode, NotificationRequestStatus status,
        Instant scheduledAt, Instant createdAt, Map<String, Long> deliveryCountsByStatus
) {

    /** Used right after create/list, before any delivery rows necessarily exist yet. */
    public static NotificationRequestResponse withoutDeliveryCounts(NotificationRequest request) {
        return new NotificationRequestResponse(request.getId(), request.getTenantId(), request.getTemplateCode(),
                request.getStatus(), request.getScheduledAt(), request.getCreatedAt(), Map.of());
    }

    public static NotificationRequestResponse withDeliveryCounts(NotificationRequest request,
                                                                  Map<String, Long> deliveryCountsByStatus) {
        return new NotificationRequestResponse(request.getId(), request.getTenantId(), request.getTemplateCode(),
                request.getStatus(), request.getScheduledAt(), request.getCreatedAt(), deliveryCountsByStatus);
    }
}
