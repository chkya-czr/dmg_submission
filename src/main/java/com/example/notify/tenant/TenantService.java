package com.example.notify.tenant;

import com.example.notify.common.errors.ConflictException;
import com.example.notify.common.errors.ResourceNotFoundException;
import com.example.notify.security.AppUser;
import com.example.notify.security.AppUserRepository;
import com.example.notify.tenant.dto.TenantAdminCreateRequest;
import com.example.notify.tenant.dto.TenantUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository, AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Tenant create(String name) {
        if (tenantRepository.existsByName(name)) {
            throw new ConflictException("A tenant named '" + name + "' already exists");
        }
        return tenantRepository.save(new Tenant(name));
    }

    @Transactional(readOnly = true)
    public Page<Tenant> list(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Tenant get(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No such tenant: " + tenantId));
    }

    public Tenant update(String tenantId, TenantUpdateRequest request) {
        Tenant tenant = get(tenantId);
        if (request.name() != null && !request.name().isBlank() && !request.name().equals(tenant.getName())) {
            if (tenantRepository.existsByName(request.name())) {
                throw new ConflictException("A tenant named '" + request.name() + "' already exists");
            }
            tenant.rename(request.name());
        }
        if (request.status() != null) {
            tenant.changeStatus(request.status());
        }
        return tenant;
    }

    public AppUser createTenantAdmin(String tenantId, TenantAdminCreateRequest request) {
        Tenant tenant = get(tenantId);
        if (appUserRepository.existsByUsername(request.username())) {
            throw new ConflictException("A user named '" + request.username() + "' already exists");
        }
        AppUser admin = AppUser.tenantAdmin(request.username(), passwordEncoder.encode(request.password()), tenant.getId());
        return appUserRepository.save(admin);
    }
}
