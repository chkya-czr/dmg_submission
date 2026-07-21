package com.example.notify.tenant.dto;

import jakarta.validation.constraints.Size;

import com.example.notify.tenant.TenantStatus;

public record TenantUpdateRequest(
        @Size(max = 255) String name,
        TenantStatus status
) {
}
