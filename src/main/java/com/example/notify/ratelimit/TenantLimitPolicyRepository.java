package com.example.notify.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantLimitPolicyRepository extends JpaRepository<TenantLimitPolicy, String> {

    Optional<TenantLimitPolicy> findByTenantId(String tenantId);
}
