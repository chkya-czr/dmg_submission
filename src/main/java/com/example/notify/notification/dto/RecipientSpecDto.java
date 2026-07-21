package com.example.notify.notification.dto;

import com.example.notify.common.model.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RecipientSpecDto(
        @NotBlank String recipientId,
        @NotNull Channel channel,
        @NotBlank String address,
        Map<String, String> variablesOverride
) {
}
