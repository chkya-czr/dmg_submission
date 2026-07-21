package com.example.notify.dispatch;

import com.example.notify.delivery.NotificationDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Reclaims deliveries left in PROCESSING by a worker that crashed or hung mid-send, putting them
 * back to PENDING so they get retried instead of stuck forever. This is the concrete scenario the
 * version-guarded claim/complete UPDATEs guard against, even though the dispatch poll loop itself
 * is single-threaded and can't race with itself.
 */
@Component
public class StuckDeliverySweeper {

    private final NotificationDeliveryRepository deliveryRepository;
    private final Clock clock;
    private final int stuckThresholdSeconds;

    public StuckDeliverySweeper(NotificationDeliveryRepository deliveryRepository, Clock clock,
                                 @Value("${notify.dispatch.stuck-claim-threshold-seconds}") int stuckThresholdSeconds) {
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
        this.stuckThresholdSeconds = stuckThresholdSeconds;
    }

    @Scheduled(fixedDelayString = "${notify.dispatch.sweep-interval-ms}")
    public void sweep() {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(stuckThresholdSeconds);
        deliveryRepository.reclaimStuck(now, cutoff);
    }
}
