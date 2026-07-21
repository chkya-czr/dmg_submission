package com.example.notify.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Null iff role == PLATFORM_ADMIN. */
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // JPA
    }

    public static AppUser platformAdmin(String username, String passwordHash) {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID().toString();
        user.username = username;
        user.passwordHash = passwordHash;
        user.role = Role.PLATFORM_ADMIN;
        user.tenantId = null;
        Instant now = Instant.now();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public static AppUser tenantAdmin(String username, String passwordHash, String tenantId) {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID().toString();
        user.username = username;
        user.passwordHash = passwordHash;
        user.role = Role.TENANT_ADMIN;
        user.tenantId = tenantId;
        Instant now = Instant.now();
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
