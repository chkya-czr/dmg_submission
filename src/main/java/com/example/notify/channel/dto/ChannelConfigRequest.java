package com.example.notify.channel.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ChannelConfigRequest(boolean enabled, @NotNull Map<String, String> config) {
}
