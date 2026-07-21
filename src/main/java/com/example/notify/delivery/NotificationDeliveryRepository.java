package com.example.notify.delivery;

import com.example.notify.common.model.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, String> {

    Optional<NotificationDelivery> findByIdAndTenantId(String id, String tenantId);

    @Query("SELECT DISTINCT d.tenantId FROM NotificationDelivery d WHERE d.status = 'PENDING' AND d.nextAttemptAt <= :now")
    List<String> findDistinctTenantsWithDueDeliveries(@Param("now") Instant now);

    @Query("SELECT d FROM NotificationDelivery d WHERE d.tenantId = :tenantId AND d.status = 'PENDING' "
            + "AND d.nextAttemptAt <= :now ORDER BY d.nextAttemptAt ASC")
    List<NotificationDelivery> findCandidatesForTenant(@Param("tenantId") String tenantId, @Param("now") Instant now,
                                                        Pageable pageable);

    /** Atomic claim: succeeds (returns 1) only if the row is still PENDING at the expected
     * version, so a concurrent claimer or the stuck-sweeper can't both act on the same row.
     * {@code @Transactional} is required here explicitly - Spring Data does not automatically
     * wrap custom {@code @Modifying} queries in a transaction the way it does for inherited
     * CRUD methods like save()/delete(). */
    @Transactional
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'PROCESSING', d.workerId = :workerId, "
            + "d.claimedAt = :now, d.version = d.version + 1, d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = 'PENDING' AND d.version = :version")
    int claim(@Param("id") String id, @Param("workerId") String workerId, @Param("now") Instant now,
              @Param("version") long version);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'SENT', d.sentAt = :now, d.attemptCount = :attemptCount, "
            + "d.version = d.version + 1, d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = 'PROCESSING' AND d.version = :version")
    int markSent(@Param("id") String id, @Param("attemptCount") int attemptCount, @Param("now") Instant now,
                 @Param("version") long version);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'PENDING', d.attemptCount = :attemptCount, "
            + "d.nextAttemptAt = :nextAttemptAt, d.version = d.version + 1, d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = 'PROCESSING' AND d.version = :version")
    int markRetryPending(@Param("id") String id, @Param("attemptCount") int attemptCount,
                         @Param("nextAttemptAt") Instant nextAttemptAt, @Param("now") Instant now,
                         @Param("version") long version);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'FAILED', d.attemptCount = :attemptCount, "
            + "d.version = d.version + 1, d.updatedAt = :now "
            + "WHERE d.id = :id AND d.status = 'PROCESSING' AND d.version = :version")
    int markFailed(@Param("id") String id, @Param("attemptCount") int attemptCount, @Param("now") Instant now,
                   @Param("version") long version);

    /** Reclaims deliveries left in PROCESSING by a worker that crashed/hung mid-send, so they get
     * retried instead of stuck forever. Does not increment attempt_count. */
    @Transactional
    @Modifying
    @Query("UPDATE NotificationDelivery d SET d.status = 'PENDING', d.nextAttemptAt = :now, "
            + "d.version = d.version + 1, d.updatedAt = :now WHERE d.status = 'PROCESSING' AND d.claimedAt < :cutoff")
    int reclaimStuck(@Param("now") Instant now, @Param("cutoff") Instant cutoff);

    @Query("SELECT d FROM NotificationDelivery d WHERE d.tenantId = :tenantId "
            + "AND (:status IS NULL OR d.status = :status) "
            + "AND (:channel IS NULL OR d.channel = :channel) "
            + "AND (:recipientId IS NULL OR d.recipientId = :recipientId) "
            + "AND (:fromDate IS NULL OR d.createdAt >= :fromDate) "
            + "AND (:toDate IS NULL OR d.createdAt <= :toDate) "
            + "ORDER BY d.createdAt DESC")
    Page<NotificationDelivery> search(@Param("tenantId") String tenantId, @Param("status") DeliveryStatus status,
                                       @Param("channel") Channel channel, @Param("recipientId") String recipientId,
                                       @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate,
                                       Pageable pageable);

    @Query("SELECT d.status, COUNT(d) FROM NotificationDelivery d WHERE d.notificationRequestId = :requestId GROUP BY d.status")
    List<Object[]> countByStatusForRequest(@Param("requestId") String requestId);

    boolean existsByNotificationRequestIdAndRecipientIdAndChannel(String notificationRequestId, String recipientId,
                                                                   Channel channel);
}
