package com.example.notify.dispatch;

import com.example.notify.common.model.Channel;
import com.example.notify.delivery.DeliveryStatus;
import com.example.notify.delivery.NotificationDelivery;
import com.example.notify.delivery.NotificationDeliveryRepository;
import com.example.notify.notification.NotificationRequest;
import com.example.notify.notification.NotificationRequestRepository;
import com.example.notify.security.AppUser;
import com.example.notify.security.AppUserRepository;
import com.example.notify.tenant.Tenant;
import com.example.notify.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the exact race the version-guarded conditional claim UPDATE exists to prevent: many
 * threads attempting to claim the same PENDING delivery row concurrently. Suspends the test's
 * default transaction (each repository call runs in its own auto-committing transaction) so the
 * concurrent threads genuinely race against the database rather than a shared, uncommitted
 * in-memory transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClaimConflictTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotificationRequestRepository notificationRequestRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Test
    void onlyOneConcurrentClaimSucceedsOnTheSameRow() throws Exception {
        // Each repository .save() below runs in its own auto-committing transaction (the class is
        // annotated NOT_SUPPORTED, so there's no ambient test transaction to participate in), so
        // this setup data is durably visible to the concurrent threads' own transactions below.
        Tenant tenant = tenantRepository.save(new Tenant("Acme"));
        AppUser admin = appUserRepository.save(AppUser.tenantAdmin("acme-admin", "hash", tenant.getId()));
        NotificationRequest request = notificationRequestRepository.save(new NotificationRequest(tenant.getId(),
                "welcome", Map.of(), List.of(), null, null, admin.getId()));
        NotificationDelivery delivery = deliveryRepository.save(new NotificationDelivery(request.getId(),
                tenant.getId(), Channel.EMAIL, "user-1", "user1@example.com", "Subject", "Body",
                DeliveryStatus.PENDING, 5, Instant.now()));

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                awaitUninterruptibly(go);
                int rows = deliveryRepository.claim(delivery.getId(), "worker-x", Instant.now(), 0L);
                if (rows == 1) {
                    successCount.incrementAndGet();
                }
            }));
        }
        ready.await();
        go.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);

        NotificationDelivery reloaded = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DeliveryStatus.PROCESSING);
        assertThat(reloaded.getVersion()).isEqualTo(1L);
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
