package com.example.notify.delivery;

import com.example.notify.common.model.Channel;
import com.example.notify.common.web.PageResponse;
import com.example.notify.delivery.dto.DeliveryEventResponse;
import com.example.notify.delivery.dto.DeliveryResponse;
import com.example.notify.security.TenantAccessGuard;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Tenant-admin (own tenant) or platform-admin (any tenant) delivery reporting. */
@RestController
@RequestMapping("/api/tenants/{tenantId}/deliveries")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class DeliveryController {

    private final DeliveryReportService deliveryReportService;
    private final TenantAccessGuard tenantAccessGuard;

    public DeliveryController(DeliveryReportService deliveryReportService, TenantAccessGuard tenantAccessGuard) {
        this.deliveryReportService = deliveryReportService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping
    public PageResponse<DeliveryResponse> list(
            @PathVariable String tenantId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            Pageable pageable) {
        tenantAccessGuard.requireAccess(tenantId);
        return PageResponse.from(deliveryReportService
                .search(tenantId, status, channel, recipientId, fromDate, toDate, pageable)
                .map(DeliveryResponse::summary));
    }

    @GetMapping("/{deliveryId}")
    public DeliveryResponse get(@PathVariable String tenantId, @PathVariable String deliveryId) {
        tenantAccessGuard.requireAccess(tenantId);
        NotificationDelivery delivery = deliveryReportService.get(tenantId, deliveryId);
        var events = deliveryReportService.getEvents(deliveryId).stream().map(DeliveryEventResponse::from).toList();
        return DeliveryResponse.from(delivery, events);
    }
}
