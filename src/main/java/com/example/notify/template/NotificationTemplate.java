package com.example.notify.template;

import com.example.notify.common.model.Channel;
import com.example.notify.common.persistence.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notification_template")
public class NotificationTemplate {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(name = "subject_template")
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false)
    private String bodyTemplate;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "variables_schema", nullable = false)
    private List<String> variablesSchema;

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationTemplate() {
        // JPA
    }

    public NotificationTemplate(String tenantId, String code, Channel channel, String subjectTemplate,
                                 String bodyTemplate, List<String> variablesSchema) {
        this.id = UUID.randomUUID().toString();
        this.tenantId = tenantId;
        this.code = code;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.variablesSchema = variablesSchema;
        this.version = 1;
        this.active = true;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String subjectTemplate, String bodyTemplate, List<String> variablesSchema) {
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.variablesSchema = variablesSchema;
        this.version += 1;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public List<String> getVariablesSchema() {
        return variablesSchema;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
