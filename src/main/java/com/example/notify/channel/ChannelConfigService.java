package com.example.notify.channel;

import com.example.notify.common.model.Channel;
import com.example.notify.tenant.TenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ChannelConfigService {

    private final ChannelConfigRepository channelConfigRepository;
    private final TenantService tenantService;

    public ChannelConfigService(ChannelConfigRepository channelConfigRepository, TenantService tenantService) {
        this.channelConfigRepository = channelConfigRepository;
        this.tenantService = tenantService;
    }

    @Transactional(readOnly = true)
    public List<ChannelConfig> list(String tenantId) {
        return channelConfigRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public EffectiveChannelConfig getEffective(String tenantId, Channel channel) {
        return channelConfigRepository.findByTenantIdAndChannel(tenantId, channel)
                .map(c -> new EffectiveChannelConfig(c.isEnabled(), c.getConfig()))
                .orElse(EffectiveChannelConfig.DEFAULT);
    }

    public ChannelConfig upsert(String tenantId, Channel channel, boolean enabled, Map<String, String> config) {
        tenantService.get(tenantId); // 404s if the tenant doesn't exist
        ChannelConfig existing = channelConfigRepository.findByTenantIdAndChannel(tenantId, channel).orElse(null);
        if (existing == null) {
            return channelConfigRepository.save(new ChannelConfig(tenantId, channel, enabled, config));
        }
        existing.update(enabled, config);
        return existing;
    }
}
