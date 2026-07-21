package com.example.notify.settings;

import com.example.notify.settings.dto.GlobalSettingRequest;
import com.example.notify.settings.dto.GlobalSettingResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-wide default rate limit / retry settings, editable only by platform admins. */
@RestController
@RequestMapping("/api/platform/settings")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class GlobalSettingController {

    private final GlobalSettingService globalSettingService;

    public GlobalSettingController(GlobalSettingService globalSettingService) {
        this.globalSettingService = globalSettingService;
    }

    @GetMapping
    public GlobalSettingResponse get() {
        return GlobalSettingResponse.from(globalSettingService.get());
    }

    @PutMapping
    public GlobalSettingResponse update(@Valid @RequestBody GlobalSettingRequest request) {
        return GlobalSettingResponse.from(globalSettingService.update(
                request.rateLimitPerMinute(), request.burstCapacity(), request.maxRetryAttempts()));
    }
}
