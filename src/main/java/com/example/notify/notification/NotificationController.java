package com.example.notify.notification;

import com.example.notify.common.web.PageResponse;
import com.example.notify.delivery.DeliveryReportService;
import com.example.notify.notification.dto.NotificationRequestResponse;
import com.example.notify.notification.dto.SendNotificationRequest;
import com.example.notify.security.TenantAccessGuard;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-admin (own tenant) or platform-admin (any tenant) send + query flows. */
@RestController
@RequestMapping("/api/tenants/{tenantId}/notifications")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;
    private final DeliveryReportService deliveryReportService;
    private final TenantAccessGuard tenantAccessGuard;

    public NotificationController(NotificationService notificationService, DeliveryReportService deliveryReportService,
                                   TenantAccessGuard tenantAccessGuard) {
        this.notificationService = notificationService;
        this.deliveryReportService = deliveryReportService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<NotificationRequestResponse> send(
            @PathVariable String tenantId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request) {
        tenantAccessGuard.requireAccess(tenantId);
        String userId = tenantAccessGuard.currentPrincipal().getUserId();
        SendOutcome outcome = notificationService.send(tenantId, request.templateCode(), request.variables(),
                request.scheduledAt(), request.recipients(), idempotencyKey, userId);
        HttpStatus status = outcome.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(NotificationRequestResponse.withoutDeliveryCounts(outcome.request()));
    }

    @GetMapping
    public PageResponse<NotificationRequestResponse> list(@PathVariable String tenantId, Pageable pageable) {
        tenantAccessGuard.requireAccess(tenantId);
        return PageResponse.from(
                notificationService.list(tenantId, pageable).map(NotificationRequestResponse::withoutDeliveryCounts));
    }

    @GetMapping("/{requestId}")
    public NotificationRequestResponse get(@PathVariable String tenantId, @PathVariable String requestId) {
        tenantAccessGuard.requireAccess(tenantId);
        NotificationRequest request = notificationService.get(tenantId, requestId);
        return NotificationRequestResponse.withDeliveryCounts(request,
                deliveryReportService.countByStatusForRequest(request.getId()));
    }
}
