package com.example.notify.security;

import com.example.notify.common.errors.TenantMismatchException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces that a {@code TENANT_ADMIN} can only act on their own tenant. Platform admins bypass
 * the check entirely. Called explicitly at the top of every tenant-scoped controller method
 * (rather than via a role annotation alone) because {@code @PreAuthorize("hasRole(...)")} can't
 * express "and the path tenantId must equal my tenantId" on its own.
 */
@Component
public class TenantAccessGuard {

    public void requireAccess(String tenantId) {
        AppUserPrincipal principal = currentPrincipal();
        if (principal.isPlatformAdmin()) {
            return;
        }
        if (!tenantId.equals(principal.getTenantId())) {
            throw new TenantMismatchException("You do not have access to tenant " + tenantId);
        }
    }

    public AppUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new IllegalStateException("No authenticated AppUserPrincipal in the security context");
        }
        return principal;
    }
}
