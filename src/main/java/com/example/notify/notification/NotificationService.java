package com.example.notify.notification;

import com.example.notify.common.errors.ResourceNotFoundException;
import com.example.notify.common.model.Channel;
import com.example.notify.notification.dto.RecipientSpecDto;
import com.example.notify.template.TemplateService;
import com.example.notify.tenant.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class NotificationService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final TemplateService templateService;
    private final TenantService tenantService;

    public NotificationService(NotificationRequestRepository notificationRequestRepository,
                                TemplateService templateService, TenantService tenantService) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.templateService = templateService;
        this.tenantService = tenantService;
    }

    public SendOutcome send(String tenantId, String templateCode, Map<String, String> variables,
                             Instant scheduledAt, List<RecipientSpecDto> recipients, String idempotencyKey,
                             String createdByUserId) {
        tenantService.get(tenantId); // 404s if the tenant doesn't exist

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = notificationRequestRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
            if (existing.isPresent()) {
                return new SendOutcome(existing.get(), true);
            }
        }

        // Fail fast if any channel referenced by the recipients has no active template yet -
        // catches a typo'd templateCode immediately rather than silently failing every delivery
        // at expansion time.
        Set<Channel> channelsUsed = new HashSet<>();
        for (RecipientSpecDto recipient : recipients) {
            channelsUsed.add(recipient.channel());
        }
        for (Channel channel : channelsUsed) {
            templateService.getActiveByCodeAndChannel(tenantId, templateCode, channel);
        }

        List<RecipientSpec> recipientSpecs = recipients.stream()
                .map(r -> new RecipientSpec(r.recipientId(), r.channel(), r.address(), r.variablesOverride()))
                .toList();

        NotificationRequest request = new NotificationRequest(tenantId, templateCode,
                variables == null ? new HashMap<>() : variables, recipientSpecs, scheduledAt,
                (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey, createdByUserId);
        return new SendOutcome(notificationRequestRepository.save(request), false);
    }

    @Transactional(readOnly = true)
    public Page<NotificationRequest> list(String tenantId, Pageable pageable) {
        return notificationRequestRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public NotificationRequest get(String tenantId, String requestId) {
        return notificationRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No such notification request: " + requestId));
    }
}
