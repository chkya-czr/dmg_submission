package com.example.notify.channel;

import com.example.notify.channel.dto.ChannelConfigRequest;
import com.example.notify.channel.dto.ChannelConfigResponse;
import com.example.notify.common.model.Channel;
import com.example.notify.security.TenantAccessGuard;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tenant-admin (own tenant) or platform-admin (any tenant) management of per-channel config. */
@RestController
@RequestMapping("/api/tenants/{tenantId}/channels")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class ChannelConfigController {

    private final ChannelConfigService channelConfigService;
    private final TenantAccessGuard tenantAccessGuard;

    public ChannelConfigController(ChannelConfigService channelConfigService, TenantAccessGuard tenantAccessGuard) {
        this.channelConfigService = channelConfigService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping
    public List<ChannelConfigResponse> list(@PathVariable String tenantId) {
        tenantAccessGuard.requireAccess(tenantId);
        return channelConfigService.list(tenantId).stream().map(ChannelConfigResponse::from).toList();
    }

    @PutMapping("/{channel}")
    public ChannelConfigResponse update(@PathVariable String tenantId, @PathVariable Channel channel,
                                         @Valid @RequestBody ChannelConfigRequest request) {
        tenantAccessGuard.requireAccess(tenantId);
        return ChannelConfigResponse.from(
                channelConfigService.upsert(tenantId, channel, request.enabled(), request.config()));
    }
}
