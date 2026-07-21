package com.example.notify.notification;

import com.example.notify.common.model.Channel;

import java.util.Map;

/** One fan-out target within a send request: a recipient on a specific channel, with an optional
 * per-recipient variable override merged on top of the shared request variables. */
public record RecipientSpec(String recipientId, Channel channel, String address, Map<String, String> variablesOverride) {
}
