package com.example.notify.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared behavior for the simulated channel senders: logs the attempt and fails ("transient",
 * retryable) according to channel_config, in priority order:
 * <ol>
 *   <li>{@code alwaysFail=true} - permanent, non-retryable failure (e.g. simulates a bad address)</li>
 *   <li>{@code failUntilAttempt=N} - deterministically fails (retryable) while the attempt number
 *       is &lt;= N, then succeeds - lets tests exercise "fails twice then succeeds" without
 *       depending on randomness</li>
 *   <li>{@code failureRate=0..1} (or {@link #defaultFailureRate} if unset) - random transient failure</li>
 * </ol>
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

        String failUntilAttempt = config.get("failUntilAttempt");
        if (failUntilAttempt != null) {
            int threshold = Integer.parseInt(failUntilAttempt);
            if (request.attemptNumber() <= threshold) {
                log.info("[{}] simulated deterministic failure (attempt {} <= failUntilAttempt {}) to={} attemptToken={}",
                        channel(), request.attemptNumber(), threshold, request.recipientAddress(), request.attemptToken());
                return SendResult.retryableFailure(
                        "Simulated deterministic failure (attempt " + request.attemptNumber() + " <= failUntilAttempt=" + threshold + ")");
            }
            log.info("[{}] sent to={} subject={} attemptToken={}", channel(), request.recipientAddress(),
                    request.subject(), request.attemptToken());
            return SendResult.ok();
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
