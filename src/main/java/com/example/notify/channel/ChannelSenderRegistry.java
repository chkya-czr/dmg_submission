package com.example.notify.channel;

import com.example.notify.common.model.Channel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks up the {@link ChannelSender} for a given {@link Channel}. Swapping a simulated sender for
 * a real provider integration is just registering another {@code ChannelSender} bean for that
 * channel - no changes needed here. */
@Component
public class ChannelSenderRegistry {

    private final Map<Channel, ChannelSender> sendersByChannel;

    public ChannelSenderRegistry(List<ChannelSender> senders) {
        this.sendersByChannel = senders.stream().collect(Collectors.toMap(ChannelSender::channel, Function.identity()));
    }

    public ChannelSender get(Channel channel) {
        ChannelSender sender = sendersByChannel.get(channel);
        if (sender == null) {
            throw new IllegalStateException("No ChannelSender registered for channel " + channel);
        }
        return sender;
    }
}
