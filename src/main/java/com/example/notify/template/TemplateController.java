package com.example.notify.template;

import com.example.notify.common.web.PageResponse;
import com.example.notify.security.TenantAccessGuard;
import com.example.notify.template.dto.TemplateCreateRequest;
import com.example.notify.template.dto.TemplateResponse;
import com.example.notify.template.dto.TemplateUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-admin (own tenant) or platform-admin (any tenant) management of notification templates. */
@RestController
@RequestMapping("/api/tenants/{tenantId}/templates")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class TemplateController {

    private final TemplateService templateService;
    private final TenantAccessGuard tenantAccessGuard;

    public TemplateController(TemplateService templateService, TenantAccessGuard tenantAccessGuard) {
        this.templateService = templateService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@PathVariable String tenantId,
                                                    @Valid @RequestBody TemplateCreateRequest request) {
        tenantAccessGuard.requireAccess(tenantId);
        NotificationTemplate template = templateService.create(tenantId, request.code(), request.channel(),
                request.subjectTemplate(), request.bodyTemplate(), request.variablesSchema());
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(template));
    }

    @GetMapping
    public PageResponse<TemplateResponse> list(@PathVariable String tenantId, Pageable pageable) {
        tenantAccessGuard.requireAccess(tenantId);
        return PageResponse.from(templateService.list(tenantId, pageable).map(TemplateResponse::from));
    }

    @GetMapping("/{templateId}")
    public TemplateResponse get(@PathVariable String tenantId, @PathVariable String templateId) {
        tenantAccessGuard.requireAccess(tenantId);
        return TemplateResponse.from(templateService.get(tenantId, templateId));
    }

    @PutMapping("/{templateId}")
    public TemplateResponse update(@PathVariable String tenantId, @PathVariable String templateId,
                                    @Valid @RequestBody TemplateUpdateRequest request) {
        tenantAccessGuard.requireAccess(tenantId);
        NotificationTemplate template = templateService.update(tenantId, templateId, request.subjectTemplate(),
                request.bodyTemplate(), request.variablesSchema());
        return TemplateResponse.from(template);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deactivate(@PathVariable String tenantId, @PathVariable String templateId) {
        tenantAccessGuard.requireAccess(tenantId);
        templateService.deactivate(tenantId, templateId);
        return ResponseEntity.noContent().build();
    }
}
