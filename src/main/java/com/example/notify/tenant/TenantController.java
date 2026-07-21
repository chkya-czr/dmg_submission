package com.example.notify.tenant;

import com.example.notify.common.web.PageResponse;
import com.example.notify.tenant.dto.AppUserResponse;
import com.example.notify.tenant.dto.TenantAdminCreateRequest;
import com.example.notify.tenant.dto.TenantCreateRequest;
import com.example.notify.tenant.dto.TenantResponse;
import com.example.notify.tenant.dto.TenantUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-admin-only tenant lifecycle management. */
@RestController
@RequestMapping("/api/platform/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantCreateRequest request) {
        Tenant tenant = tenantService.create(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @GetMapping
    public PageResponse<TenantResponse> list(Pageable pageable) {
        return PageResponse.from(tenantService.list(pageable).map(TenantResponse::from));
    }

    @GetMapping("/{tenantId}")
    public TenantResponse get(@PathVariable String tenantId) {
        return TenantResponse.from(tenantService.get(tenantId));
    }

    @PatchMapping("/{tenantId}")
    public TenantResponse update(@PathVariable String tenantId, @Valid @RequestBody TenantUpdateRequest request) {
        return TenantResponse.from(tenantService.update(tenantId, request));
    }

    @PostMapping("/{tenantId}/admins")
    public ResponseEntity<AppUserResponse> createTenantAdmin(@PathVariable String tenantId,
                                                              @Valid @RequestBody TenantAdminCreateRequest request) {
        var admin = tenantService.createTenantAdmin(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppUserResponse.from(admin));
    }
}
