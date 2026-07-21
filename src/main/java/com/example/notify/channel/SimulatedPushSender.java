package com.example.notify.channel;

import com.example.notify.common.model.Channel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedPushSender extends AbstractSimulatedSender {

    public SimulatedPushSender(@Value("${notify.channel.simulated.default-failure-rate:0.0}") double defaultFailureRate) {
        super(defaultFailureRate);
    }

    @Override
    public Channel channel() {
        return Channel.PUSH;
    }
}
