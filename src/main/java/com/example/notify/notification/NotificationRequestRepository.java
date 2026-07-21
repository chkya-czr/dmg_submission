package com.example.notify.notification;

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

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, String> {

    Optional<NotificationRequest> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    Page<NotificationRequest> findByTenantId(String tenantId, Pageable pageable);

    Optional<NotificationRequest> findByIdAndTenantId(String id, String tenantId);

    @Query("SELECT r FROM NotificationRequest r WHERE r.status IN ('PENDING', 'SCHEDULED') "
            + "AND (r.scheduledAt IS NULL OR r.scheduledAt <= :now) ORDER BY r.createdAt ASC")
    List<NotificationRequest> findDueForExpansion(@Param("now") Instant now, Pageable pageable);

    /**
     * Atomically claims a request for expansion: succeeds (returns 1) only if the row is still in
     * PENDING/SCHEDULED with the expected version, so two overlapping expansion runs can't both
     * expand the same request into duplicate deliveries.
     */
    @Transactional
    @Modifying
    @Query("UPDATE NotificationRequest r SET r.status = 'EXPANDING', r.version = r.version + 1, r.updatedAt = :now "
            + "WHERE r.id = :id AND r.status IN ('PENDING', 'SCHEDULED') AND r.version = :version")
    int claimForExpansion(@Param("id") String id, @Param("version") long version, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationRequest r SET r.status = 'EXPANDED', r.version = r.version + 1, r.updatedAt = :now "
            + "WHERE r.id = :id AND r.status = 'EXPANDING'")
    int markExpanded(@Param("id") String id, @Param("now") Instant now);
}
