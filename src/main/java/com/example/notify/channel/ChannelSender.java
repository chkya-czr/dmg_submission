package com.example.notify.channel;

import com.example.notify.common.model.Channel;

/**
 * Pluggable per-channel delivery mechanism. The default implementations ({@code Simulated*Sender})
 * log the send and can be configured with a random failure rate to exercise retry/backoff without
 * needing real provider credentials. A real integration (SMTP/Twilio/FCM) is just another
 * implementation of this interface, registered for the same {@link Channel}.
 */
public interface ChannelSender {

    Channel channel();

    SendResult send(SendRequest request);
}
