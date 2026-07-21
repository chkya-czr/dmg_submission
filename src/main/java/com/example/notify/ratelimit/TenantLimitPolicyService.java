package com.example.notify.ratelimit;

import com.example.notify.settings.GlobalSettingService;
import com.example.notify.tenant.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantLimitPolicyService {

    private final TenantLimitPolicyRepository policyRepository;
    private final GlobalSettingService globalSettingService;
    private final TenantService tenantService;
    private final TenantRateLimiter tenantRateLimiter;

    public TenantLimitPolicyService(TenantLimitPolicyRepository policyRepository,
                                     GlobalSettingService globalSettingService,
                                     TenantService tenantService,
                                     TenantRateLimiter tenantRateLimiter) {
        this.policyRepository = policyRepository;
        this.globalSettingService = globalSettingService;
        this.tenantService = tenantService;
        this.tenantRateLimiter = tenantRateLimiter;
    }

    @Transactional(readOnly = true)
    public TenantLimitPolicy getRaw(String tenantId) {
        return policyRepository.findByTenantId(tenantId).orElse(null);
    }

    @Transactional(readOnly = true)
    public EffectiveLimits getEffective(String tenantId) {
        var global = globalSettingService.get();
        TenantLimitPolicy policy = policyRepository.findByTenantId(tenantId).orElse(null);
        int rateLimit = (policy != null && policy.getRateLimitPerMinute() != null)
                ? policy.getRateLimitPerMinute() : global.getDefaultRateLimitPerMinute();
        int burst = (policy != null && policy.getBurstCapacity() != null)
                ? policy.getBurstCapacity() : global.getDefaultBurstCapacity();
        int maxAttempts = (policy != null && policy.getMaxRetryAttempts() != null)
                ? policy.getMaxRetryAttempts() : global.getDefaultMaxRetryAttempts();
        return new EffectiveLimits(rateLimit, burst, maxAttempts);
    }

    public TenantLimitPolicy upsert(String tenantId, Integer rateLimitPerMinute, Integer burstCapacity,
                                     Integer maxRetryAttempts) {
        tenantService.get(tenantId); // 404s if the tenant doesn't exist
        TenantLimitPolicy policy = policyRepository.findByTenantId(tenantId)
                .orElseGet(() -> new TenantLimitPolicy(tenantId));
        policy.update(rateLimitPerMinute, burstCapacity, maxRetryAttempts);
        TenantLimitPolicy saved = policyRepository.save(policy);
        tenantRateLimiter.invalidate(tenantId);
        return saved;
    }
}
