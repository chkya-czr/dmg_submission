package com.example.notify.tenant.dto;

import com.example.notify.security.AppUser;

public record AppUserResponse(String id, String username, String role, String tenantId) {

    public static AppUserResponse from(AppUser user) {
        return new AppUserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getTenantId());
    }
}
