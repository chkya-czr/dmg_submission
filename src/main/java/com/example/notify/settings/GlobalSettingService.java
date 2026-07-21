package com.example.notify.settings;

import com.example.notify.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GlobalSettingService {

    private final GlobalSettingRepository globalSettingRepository;

    public GlobalSettingService(GlobalSettingRepository globalSettingRepository) {
        this.globalSettingRepository = globalSettingRepository;
    }

    @Transactional(readOnly = true)
    public GlobalSetting get() {
        return globalSettingRepository.findById(GlobalSetting.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Global settings have not been initialized"));
    }

    public GlobalSetting update(int rateLimitPerMinute, int burstCapacity, int maxRetryAttempts) {
        GlobalSetting setting = get();
        setting.update(rateLimitPerMinute, burstCapacity, maxRetryAttempts);
        return setting;
    }
}
