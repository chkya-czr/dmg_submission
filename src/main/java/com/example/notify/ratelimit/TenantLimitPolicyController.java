package com.example.notify.ratelimit;

import com.example.notify.ratelimit.dto.TenantLimitPolicyRequest;
import com.example.notify.ratelimit.dto.TenantLimitPolicyResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-admin-only per-tenant overrides of the global rate-limit / retry defaults. */
@RestController
@RequestMapping("/api/platform/tenants/{tenantId}/limits")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantLimitPolicyController {

    private final TenantLimitPolicyService tenantLimitPolicyService;

    public TenantLimitPolicyController(TenantLimitPolicyService tenantLimitPolicyService) {
        this.tenantLimitPolicyService = tenantLimitPolicyService;
    }

    @GetMapping
    public TenantLimitPolicyResponse get(@PathVariable String tenantId) {
        TenantLimitPolicy raw = tenantLimitPolicyService.getRaw(tenantId);
        return TenantLimitPolicyResponse.of(tenantId, raw, tenantLimitPolicyService.getEffective(tenantId));
    }

    @PutMapping
    public TenantLimitPolicyResponse update(@PathVariable String tenantId,
                                             @Valid @RequestBody TenantLimitPolicyRequest request) {
        TenantLimitPolicy saved = tenantLimitPolicyService.upsert(tenantId, request.rateLimitPerMinute(),
                request.burstCapacity(), request.maxRetryAttempts());
        return TenantLimitPolicyResponse.of(tenantId, saved, tenantLimitPolicyService.getEffective(tenantId));
    }
}
