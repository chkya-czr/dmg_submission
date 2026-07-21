package com.example.notify.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared behavior for the simulated channel senders: logs the attempt and randomly fails
 * ("transient", retryable) based on a per-tenant {@code failureRate} (0..1, from channel_config),
 * falling back to {@link #defaultFailureRate}. A {@code channelConfig} value of
 * {@code alwaysFail=true} simulates a permanent, non-retryable failure (e.g. bad address).
 */
abstract class AbstractSimulatedSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(AbstractSimulatedSender.class);

    private final double defaultFailureRate;

    protected AbstractSimulatedSender(double defaultFailureRate) {
        this.defaultFailureRate = defaultFailureRate;
    }

    @Override
    public SendResult send(SendRequest request) {
        var config = request.channelConfig() == null ? java.util.Map.<String, String>of() : request.channelConfig();

        if ("true".equalsIgnoreCase(config.get("alwaysFail"))) {
            log.info("[{}] simulated permanent failure to={} attemptToken={}", channel(),
                    request.recipientAddress(), request.attemptToken());
            return SendResult.permanentFailure("Simulated permanent failure (alwaysFail=true)");
        }

        double failureRate = defaultFailureRate;
        String override = config.get("failureRate");
        if (override != null) {
            try {
                failureRate = Double.parseDouble(override);
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.info("[{}] simulated transient failure to={} attemptToken={}", channel(),
                    request.recipientAddress(), request.attemptToken());
            return SendResult.retryableFailure("Simulated transient failure (failureRate=" + failureRate + ")");
        }

        log.info("[{}] sent to={} subject={} attemptToken={}", channel(), request.recipientAddress(),
                request.subject(), request.attemptToken());
        return SendResult.ok();
    }
}
