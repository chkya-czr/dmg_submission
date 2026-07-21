package com.example.notify.channel.dto;

import com.example.notify.common.model.Channel;
import com.example.notify.channel.ChannelConfig;

import java.util.Map;

public record ChannelConfigResponse(Channel channel, boolean enabled, Map<String, String> config) {

    public static ChannelConfigResponse from(ChannelConfig config) {
        return new ChannelConfigResponse(config.getChannel(), config.isEnabled(), config.getConfig());
    }
}
