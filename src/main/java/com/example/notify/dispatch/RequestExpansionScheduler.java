package com.example.notify.dispatch;

import com.example.notify.delivery.DeliveryEvent;
import com.example.notify.delivery.DeliveryEventRepository;
import com.example.notify.delivery.DeliveryStatus;
import com.example.notify.delivery.NotificationDelivery;
import com.example.notify.delivery.NotificationDeliveryRepository;
import com.example.notify.notification.NotificationRequest;
import com.example.notify.notification.NotificationRequestRepository;
import com.example.notify.notification.RecipientSpec;
import com.example.notify.ratelimit.EffectiveLimits;
import com.example.notify.ratelimit.TenantLimitPolicyService;
import com.example.notify.template.NotificationTemplate;
import com.example.notify.template.TemplateRenderer;
import com.example.notify.template.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns due {@link NotificationRequest} rows (immediate ones right away, scheduled ones once their
 * scheduledAt passes) into one {@link NotificationDelivery} row per recipient x channel. A
 * recipient whose template/variables can't be resolved gets its own delivery row created already
 * FAILED with a descriptive audit event, instead of failing the whole batch.
 */
@Component
public class RequestExpansionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RequestExpansionScheduler.class);

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final TenantLimitPolicyService tenantLimitPolicyService;
    private final Clock clock;
    private final int batchSize;

    public RequestExpansionScheduler(NotificationRequestRepository notificationRequestRepository,
                                      NotificationDeliveryRepository deliveryRepository,
                                      DeliveryEventRepository deliveryEventRepository,
                                      TemplateService templateService,
                                      TemplateRenderer templateRenderer,
                                      TenantLimitPolicyService tenantLimitPolicyService,
                                      Clock clock,
                                      @Value("${notify.dispatch.expansion-batch-size}") int batchSize) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryEventRepository = deliveryEventRepository;
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.tenantLimitPolicyService = tenantLimitPolicyService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notify.dispatch.expansion-poll-interval-ms}")
    public void expandDue() {
        Instant now = clock.instant();
        List<NotificationRequest> due =
                notificationRequestRepository.findDueForExpansion(now, PageRequest.of(0, batchSize));
        for (NotificationRequest request : due) {
            int claimed = notificationRequestRepository.claimForExpansion(request.getId(), request.getVersion(), now);
            if (claimed == 0) {
                continue; // lost the race to another expansion run
            }
            expandOne(request.getId());
        }
    }

    private void expandOne(String requestId) {
        NotificationRequest request = notificationRequestRepository.findById(requestId).orElseThrow();
        EffectiveLimits limits = tenantLimitPolicyService.getEffective(request.getTenantId());
        Instant now = clock.instant();

        for (RecipientSpec recipient : request.getRecipients()) {
            if (deliveryRepository.existsByNotificationRequestIdAndRecipientIdAndChannel(
                    requestId, recipient.recipientId(), recipient.channel())) {
                continue; // defensive: already expanded (e.g. a previous crashed run got partway through)
            }
            expandOneRecipient(request, recipient, limits, now);
        }

        notificationRequestRepository.markExpanded(requestId, clock.instant());
    }

    private void expandOneRecipient(NotificationRequest request, RecipientSpec recipient, EffectiveLimits limits,
                                     Instant now) {
        try {
            NotificationTemplate template = templateService.getActiveByCodeAndChannel(
                    request.getTenantId(), request.getTemplateCode(), recipient.channel());

            Map<String, String> mergedVariables = new HashMap<>(request.getVariables());
            if (recipient.variablesOverride() != null) {
                mergedVariables.putAll(recipient.variablesOverride());
            }

            String subject = template.getSubjectTemplate() == null
                    ? null : templateRenderer.render(template.getSubjectTemplate(), mergedVariables);
            String body = templateRenderer.render(template.getBodyTemplate(), mergedVariables);

            deliveryRepository.save(new NotificationDelivery(request.getId(), request.getTenantId(),
                    recipient.channel(), recipient.recipientId(), recipient.address(), subject, body,
                    DeliveryStatus.PENDING, limits.maxRetryAttempts(), now));
        } catch (RuntimeException e) {
            log.warn("Failed to expand recipient {} ({}) for request {}: {}", recipient.recipientId(),
                    recipient.channel(), request.getId(), e.getMessage());
            NotificationDelivery failed = deliveryRepository.save(new NotificationDelivery(request.getId(),
                    request.getTenantId(), recipient.channel(), recipient.recipientId(), recipient.address(),
                    null, "", DeliveryStatus.FAILED, limits.maxRetryAttempts(), now));
            deliveryEventRepository.save(new DeliveryEvent(failed.getId(), null, null, DeliveryStatus.PENDING,
                    DeliveryStatus.FAILED, e.getMessage(), null));
        }
    }
}
