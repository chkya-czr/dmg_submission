package com.example.notify.dispatch;

import com.example.notify.channel.ChannelConfigService;
import com.example.notify.channel.ChannelSenderRegistry;
import com.example.notify.channel.EffectiveChannelConfig;
import com.example.notify.channel.SendRequest;
import com.example.notify.channel.SendResult;
import com.example.notify.delivery.DeliveryEvent;
import com.example.notify.delivery.DeliveryEventRepository;
import com.example.notify.delivery.DeliveryStatus;
import com.example.notify.delivery.NotificationDelivery;
import com.example.notify.delivery.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Executes exactly one delivery attempt for an already-claimed {@link NotificationDelivery}, then
 * writes the outcome via the same version-guarded conditional UPDATE pattern used to claim it -
 * if that UPDATE affects 0 rows, the stuck-delivery sweeper already reclaimed the row out from
 * under us (this worker likely ran unusually long), so the outcome is logged and dropped rather
 * than applied to a row we no longer own.
 */
@Component
public class DeliveryDispatchTask {

    private static final Logger log = LoggerFactory.getLogger(DeliveryDispatchTask.class);

    private final NotificationDeliveryRepository deliveryRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final ChannelSenderRegistry channelSenderRegistry;
    private final ChannelConfigService channelConfigService;
    private final BackoffCalculator backoffCalculator;
    private final Clock clock;

    public DeliveryDispatchTask(NotificationDeliveryRepository deliveryRepository,
                                DeliveryEventRepository deliveryEventRepository,
                                ChannelSenderRegistry channelSenderRegistry,
                                ChannelConfigService channelConfigService,
                                BackoffCalculator backoffCalculator,
                                Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryEventRepository = deliveryEventRepository;
        this.channelSenderRegistry = channelSenderRegistry;
        this.channelConfigService = channelConfigService;
        this.backoffCalculator = backoffCalculator;
        this.clock = clock;
    }

    public void dispatch(String deliveryId, long claimedVersion, String workerId) {
        NotificationDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return;
        }

        int attemptNumber = delivery.getAttemptCount() + 1;
        String attemptToken = UUID.randomUUID().toString();
        SendResult result = attempt(delivery, attemptNumber, attemptToken);
        Instant now = clock.instant();

        int rowsAffected;
        DeliveryStatus toStatus;
        if (result.success()) {
            rowsAffected = deliveryRepository.markSent(deliveryId, attemptNumber, now, claimedVersion);
            toStatus = DeliveryStatus.SENT;
        } else if (result.retryable() && attemptNumber < delivery.getMaxAttempts()) {
            Instant nextAttemptAt = backoffCalculator.computeNextAttemptAt(now, attemptNumber);
            rowsAffected = deliveryRepository.markRetryPending(deliveryId, attemptNumber, nextAttemptAt, now, claimedVersion);
            toStatus = DeliveryStatus.PENDING;
        } else {
            rowsAffected = deliveryRepository.markFailed(deliveryId, attemptNumber, now, claimedVersion);
            toStatus = DeliveryStatus.FAILED;
        }

        if (rowsAffected == 0) {
            log.warn("Delivery {} was reclaimed by the stuck-delivery sweeper before attempt {} completed; "
                    + "dropping this outcome so we don't clobber the reclaimed state.", deliveryId, attemptNumber);
            return;
        }

        deliveryEventRepository.save(new DeliveryEvent(deliveryId, attemptNumber, attemptToken,
                DeliveryStatus.PROCESSING, toStatus, result.errorMessage(), workerId));
    }

    private SendResult attempt(NotificationDelivery delivery, int attemptNumber, String attemptToken) {
        EffectiveChannelConfig channelConfig = channelConfigService.getEffective(delivery.getTenantId(), delivery.getChannel());
        if (!channelConfig.enabled()) {
            return SendResult.retryableFailure("Channel " + delivery.getChannel() + " is disabled for this tenant");
        }
        try {
            SendRequest request = new SendRequest(delivery.getRecipientAddress(), delivery.getRenderedSubject(),
                    delivery.getRenderedBody(), attemptToken, attemptNumber, channelConfig.config());
            return channelSenderRegistry.get(delivery.getChannel()).send(request);
        } catch (Exception e) {
            log.warn("ChannelSender threw for delivery {}", delivery.getId(), e);
            return SendResult.retryableFailure("Sender threw: " + e.getMessage());
        }
    }
}
