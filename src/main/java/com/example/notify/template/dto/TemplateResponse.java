package com.example.notify.template.dto;

import com.example.notify.common.model.Channel;
import com.example.notify.template.NotificationTemplate;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(
        String id, String tenantId, String code, Channel channel, String subjectTemplate, String bodyTemplate,
        List<String> variablesSchema, int version, boolean active, Instant createdAt, Instant updatedAt
) {

    public static TemplateResponse from(NotificationTemplate template) {
        return new TemplateResponse(template.getId(), template.getTenantId(), template.getCode(),
                template.getChannel(), template.getSubjectTemplate(), template.getBodyTemplate(),
                template.getVariablesSchema(), template.getVersion(), template.isActive(),
                template.getCreatedAt(), template.getUpdatedAt());
    }
}
