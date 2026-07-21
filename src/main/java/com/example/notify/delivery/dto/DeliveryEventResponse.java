package com.example.notify.delivery.dto;

import com.example.notify.delivery.DeliveryEvent;

import java.time.Instant;

public record DeliveryEventResponse(Integer attemptNumber, String fromStatus, String toStatus, String errorMessage,
                                     Instant createdAt) {

    public static DeliveryEventResponse from(DeliveryEvent event) {
        return new DeliveryEventResponse(event.getAttemptNumber(), event.getFromStatus(), event.getToStatus(),
                event.getErrorMessage(), event.getCreatedAt());
    }
}
