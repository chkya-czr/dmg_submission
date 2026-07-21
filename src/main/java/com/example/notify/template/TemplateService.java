package com.example.notify.template;

import com.example.notify.common.errors.BadRequestException;
import com.example.notify.common.errors.ConflictException;
import com.example.notify.common.errors.ResourceNotFoundException;
import com.example.notify.common.model.Channel;
import com.example.notify.tenant.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateRenderer templateRenderer;
    private final TenantService tenantService;

    public TemplateService(NotificationTemplateRepository templateRepository, TemplateRenderer templateRenderer,
                            TenantService tenantService) {
        this.templateRepository = templateRepository;
        this.templateRenderer = templateRenderer;
        this.tenantService = tenantService;
    }

    public NotificationTemplate create(String tenantId, String code, Channel channel, String subjectTemplate,
                                        String bodyTemplate, List<String> variablesSchema) {
        tenantService.get(tenantId); // 404s if the tenant doesn't exist
        if (channel == Channel.EMAIL && (subjectTemplate == null || subjectTemplate.isBlank())) {
            throw new BadRequestException("EMAIL templates require a subjectTemplate");
        }
        if (templateRepository.existsByTenantIdAndCodeAndChannel(tenantId, code, channel)) {
            throw new ConflictException("A template with code '" + code + "' already exists for channel " + channel);
        }
        templateRenderer.validateSchema(variablesSchema, subjectTemplate, bodyTemplate);
        NotificationTemplate template = new NotificationTemplate(tenantId, code, channel, subjectTemplate,
                bodyTemplate, variablesSchema);
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public Page<NotificationTemplate> list(String tenantId, Pageable pageable) {
        return templateRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public NotificationTemplate get(String tenantId, String templateId) {
        return templateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No such template: " + templateId));
    }

    @Transactional(readOnly = true)
    public NotificationTemplate getActiveByCodeAndChannel(String tenantId, String code, Channel channel) {
        NotificationTemplate template = templateRepository.findByTenantIdAndCodeAndChannel(tenantId, code, channel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No template '" + code + "' for channel " + channel + " in tenant " + tenantId));
        if (!template.isActive()) {
            throw new ResourceNotFoundException(
                    "Template '" + code + "' for channel " + channel + " is not active");
        }
        return template;
    }

    public NotificationTemplate update(String tenantId, String templateId, String subjectTemplate,
                                        String bodyTemplate, List<String> variablesSchema) {
        NotificationTemplate template = get(tenantId, templateId);
        if (template.getChannel() == Channel.EMAIL && (subjectTemplate == null || subjectTemplate.isBlank())) {
            throw new BadRequestException("EMAIL templates require a subjectTemplate");
        }
        templateRenderer.validateSchema(variablesSchema, subjectTemplate, bodyTemplate);
        template.update(subjectTemplate, bodyTemplate, variablesSchema);
        return template;
    }

    public void deactivate(String tenantId, String templateId) {
        NotificationTemplate template = get(tenantId, templateId);
        template.deactivate();
    }
}
