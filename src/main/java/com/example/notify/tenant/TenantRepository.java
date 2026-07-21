package com.example.notify.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    boolean existsByName(String name);

    Optional<Tenant> findByName(String name);
}
