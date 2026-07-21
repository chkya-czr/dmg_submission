package com.example.notify.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(
        @NotBlank @Size(max = 255) String name
) {
}
