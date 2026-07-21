package com.example.notify.channel;

import java.util.Map;

/** Everything a {@link ChannelSender} needs to attempt one delivery. {@code attemptToken} is the
 * idempotency key a real provider (Twilio/SES/FCM) would dedupe on; the simulated senders just log
 * it. {@code attemptNumber} lets the simulated senders support deterministic "fail N times then
 * succeed" test setups via channel_config, since a purely random failureRate can't reliably do that. */
public record SendRequest(String recipientAddress, String subject, String body, String attemptToken,
                           int attemptNumber, Map<String, String> channelConfig) {
}
