package com.example.notify.security;

/** The two roles defined by the requirement. Platform admins manage tenants and global limits;
 * tenant admins manage templates/channel config/reports scoped to their own tenant. */
public enum Role {
    PLATFORM_ADMIN,
    TENANT_ADMIN
}
