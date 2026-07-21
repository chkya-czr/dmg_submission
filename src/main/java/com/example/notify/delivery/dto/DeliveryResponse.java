package com.example.notify.delivery.dto;

import com.example.notify.common.model.Channel;
import com.example.notify.delivery.DeliveryStatus;
import com.example.notify.delivery.NotificationDelivery;

import java.time.Instant;
import java.util.List;

public record DeliveryResponse(
        String id, String notificationRequestId, Channel channel, String recipientId, String recipientAddress,
        DeliveryStatus status, int attemptCount, int maxAttempts, Instant nextAttemptAt, Instant sentAt,
        Instant createdAt, List<DeliveryEventResponse> events
) {

    public static DeliveryResponse from(NotificationDelivery delivery, List<DeliveryEventResponse> events) {
        return new DeliveryResponse(delivery.getId(), delivery.getNotificationRequestId(), delivery.getChannel(),
                delivery.getRecipientId(), delivery.getRecipientAddress(), delivery.getStatus(),
                delivery.getAttemptCount(), delivery.getMaxAttempts(), delivery.getNextAttemptAt(),
                delivery.getSentAt(), delivery.getCreatedAt(), events);
    }

    public static DeliveryResponse summary(NotificationDelivery delivery) {
        return from(delivery, null);
    }
}
