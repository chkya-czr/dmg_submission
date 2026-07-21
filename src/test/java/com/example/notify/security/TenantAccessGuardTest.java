package com.example.notify.security;

import com.example.notify.common.errors.TenantMismatchException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class TenantAccessGuardTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(AppUser user) {
        AppUserPrincipal principal = new AppUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void tenantAdminCanAccessOwnTenant() {
        authenticateAs(AppUser.tenantAdmin("alice", "hash", "tenant-1"));
        assertThatCode(() -> guard.requireAccess("tenant-1")).doesNotThrowAnyException();
    }

    @Test
    void tenantAdminCannotAccessAnotherTenant() {
        authenticateAs(AppUser.tenantAdmin("alice", "hash", "tenant-1"));
        assertThatThrownBy(() -> guard.requireAccess("tenant-2"))
                .isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void platformAdminBypassesTheCheckForAnyTenant() {
        authenticateAs(AppUser.platformAdmin("root", "hash"));
        assertThatCode(() -> guard.requireAccess("tenant-anything")).doesNotThrowAnyException();
    }

    @Test
    void currentPrincipalReturnsTheAuthenticatedUser() {
        authenticateAs(AppUser.tenantAdmin("alice", "hash", "tenant-1"));
        assertThat(guard.currentPrincipal().getUsername()).isEqualTo("alice");
    }
}
