package com.example.notify.tenant.dto;

import com.example.notify.tenant.Tenant;
import com.example.notify.tenant.TenantStatus;

import java.time.Instant;

public record TenantResponse(String id, String name, TenantStatus status, Instant createdAt, Instant updatedAt) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getStatus(),
                tenant.getCreatedAt(), tenant.getUpdatedAt());
    }
}
