package com.example.notify.dispatch;

import com.example.notify.delivery.NotificationDelivery;
import com.example.notify.delivery.NotificationDeliveryRepository;
import com.example.notify.ratelimit.EffectiveLimits;
import com.example.notify.ratelimit.TenantLimitPolicyService;
import com.example.notify.ratelimit.TenantRateLimiter;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * The core dispatch loop. Every poll: finds tenants with due PENDING deliveries, round-robins
 * through them (see {@link TenantFairnessRotator}), and for each tenant in turn checks the
 * per-tenant rate limit before attempting to atomically claim one delivery row. A successful
 * claim is handed off to the bounded worker pool ({@link DeliveryDispatchTask}); the pool permit
 * is only released once that async task finishes, which is what actually bounds in-flight work
 * to the pool size.
 */
@Component
public class DispatchScheduler {

    private final NotificationDeliveryRepository deliveryRepository;
    private final TenantLimitPolicyService tenantLimitPolicyService;
    private final TenantRateLimiter tenantRateLimiter;
    private final TenantFairnessRotator tenantFairnessRotator;
    private final DeliveryDispatchTask deliveryDispatchTask;
    private final ExecutorService dispatchExecutor;
    private final Semaphore dispatchSemaphore;
    private final Clock clock;
    private final String workerId = "worker-" + UUID.randomUUID();

    public DispatchScheduler(NotificationDeliveryRepository deliveryRepository,
                              TenantLimitPolicyService tenantLimitPolicyService,
                              TenantRateLimiter tenantRateLimiter,
                              TenantFairnessRotator tenantFairnessRotator,
                              DeliveryDispatchTask deliveryDispatchTask,
                              ExecutorService dispatchExecutor,
                              Semaphore dispatchSemaphore,
                              Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.tenantLimitPolicyService = tenantLimitPolicyService;
        this.tenantRateLimiter = tenantRateLimiter;
        this.tenantFairnessRotator = tenantFairnessRotator;
        this.deliveryDispatchTask = deliveryDispatchTask;
        this.dispatchExecutor = dispatchExecutor;
        this.dispatchSemaphore = dispatchSemaphore;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${notify.dispatch.dispatch-poll-interval-ms}")
    public void dispatchDue() {
        Instant now = clock.instant();
        List<String> tenantIds = deliveryRepository.findDistinctTenantsWithDueDeliveries(now);
        if (tenantIds.isEmpty()) {
            return;
        }
        List<String> rotated = tenantFairnessRotator.rotate(tenantIds);

        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (String tenantId : rotated) {
                if (!dispatchSemaphore.tryAcquire()) {
                    return; // pool exhausted - resume next poll
                }
                if (tryClaimAndDispatchOne(tenantId, now)) {
                    progressed = true;
                } else {
                    dispatchSemaphore.release();
                }
            }
        }
    }

    /** Returns true iff a delivery was claimed and handed to the worker pool (which then owns
     * releasing the semaphore permit); false means the caller must release the permit itself. */
    private boolean tryClaimAndDispatchOne(String tenantId, Instant now) {
        EffectiveLimits limits = tenantLimitPolicyService.getEffective(tenantId);
        if (!tenantRateLimiter.tryConsume(tenantId, limits)) {
            return false;
        }

        List<NotificationDelivery> candidates =
                deliveryRepository.findCandidatesForTenant(tenantId, now, PageRequest.of(0, 1));
        if (candidates.isEmpty()) {
            tenantRateLimiter.refund(tenantId, limits);
            return false;
        }

        NotificationDelivery candidate = candidates.get(0);
        long claimedVersion = candidate.getVersion() + 1;
        int claimed = deliveryRepository.claim(candidate.getId(), workerId, now, candidate.getVersion());
        if (claimed == 0) {
            tenantRateLimiter.refund(tenantId, limits);
            return false;
        }

        String deliveryId = candidate.getId();
        dispatchExecutor.submit(() -> {
            try {
                deliveryDispatchTask.dispatch(deliveryId, claimedVersion, workerId);
            } finally {
                dispatchSemaphore.release();
            }
        });
        return true;
    }
}
