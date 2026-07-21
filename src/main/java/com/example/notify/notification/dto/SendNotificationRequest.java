package com.example.notify.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SendNotificationRequest(
        @NotBlank String templateCode,
        Map<String, String> variables,
        @Future Instant scheduledAt,
        @NotEmpty @Valid List<RecipientSpecDto> recipients
) {
}
