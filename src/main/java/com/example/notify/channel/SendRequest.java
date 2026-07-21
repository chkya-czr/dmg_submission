package com.example.notify.channel;

import java.util.Map;

/** Everything a {@link ChannelSender} needs to attempt one delivery. {@code attemptToken} is the
 * idempotency key a real provider (Twilio/SES/FCM) would dedupe on; the simulated senders just log it. */
public record SendRequest(String recipientAddress, String subject, String body, String attemptToken,
                           Map<String, String> channelConfig) {
}
