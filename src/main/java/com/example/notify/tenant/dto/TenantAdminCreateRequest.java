package com.example.notify.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantAdminCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,100}$") String username,
        @NotBlank @Size(min = 8, max = 200) String password
) {
}
