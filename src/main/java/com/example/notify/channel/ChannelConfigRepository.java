package com.example.notify.channel;

import com.example.notify.common.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, String> {

    Optional<ChannelConfig> findByTenantIdAndChannel(String tenantId, Channel channel);

    List<ChannelConfig> findByTenantId(String tenantId);
}
