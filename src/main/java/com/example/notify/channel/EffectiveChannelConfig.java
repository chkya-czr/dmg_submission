package com.example.notify.channel;

import java.util.Map;

/** Resolved channel configuration for a tenant: an explicit {@link ChannelConfig} row if one
 * exists, otherwise the default (enabled, no overrides) so every channel works out of the box. */
public record EffectiveChannelConfig(boolean enabled, Map<String, String> config) {

    public static final EffectiveChannelConfig DEFAULT = new EffectiveChannelConfig(true, Map.of());
}
