-- Core schema for the multi-tenant notification service.
-- Written in portable ANSI SQL (app-generated UUID/VARCHAR(36) primary keys, TEXT instead of
-- CLOB, no vendor-specific functions) so it runs unmodified on H2 or PostgreSQL.

CREATE TABLE tenant (
    id           VARCHAR(36)     NOT NULL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT uq_tenant_name UNIQUE (name),
    CONSTRAINT ck_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE app_user (
    id            VARCHAR(36)     NOT NULL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    tenant_id     VARCHAR(36)     NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT ck_app_user_role CHECK (role IN ('PLATFORM_ADMIN', 'TENANT_ADMIN')),
    CONSTRAINT ck_app_user_role_tenant CHECK (
        (role = 'PLATFORM_ADMIN' AND tenant_id IS NULL) OR
        (role = 'TENANT_ADMIN' AND tenant_id IS NOT NULL)
    )
);

CREATE TABLE global_setting (
    id                             VARCHAR(36)  NOT NULL PRIMARY KEY,
    default_rate_limit_per_minute INT       NOT NULL,
    default_burst_capacity        INT       NOT NULL,
    default_max_retry_attempts    INT       NOT NULL,
    updated_at                     TIMESTAMP NOT NULL
);

CREATE TABLE tenant_limit_policy (
    id                     VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id              VARCHAR(36) NOT NULL,
    rate_limit_per_minute  INT      NULL,
    burst_capacity         INT      NULL,
    max_retry_attempts     INT      NULL,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL,
    CONSTRAINT uq_tenant_limit_policy_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_tenant_limit_policy_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE TABLE notification_template (
    id                VARCHAR(36)     NOT NULL PRIMARY KEY,
    tenant_id         VARCHAR(36)     NOT NULL,
    code              VARCHAR(100) NOT NULL,
    channel           VARCHAR(20)  NOT NULL,
    subject_template  VARCHAR(500) NULL,
    body_template     TEXT         NOT NULL,
    variables_schema  TEXT         NOT NULL,
    version           INT          NOT NULL DEFAULT 1,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_template_tenant_code_channel UNIQUE (tenant_id, code, channel),
    CONSTRAINT fk_template_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT ck_template_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT ck_template_email_subject CHECK (channel <> 'EMAIL' OR subject_template IS NOT NULL)
);

CREATE TABLE channel_config (
    id          VARCHAR(36)    NOT NULL PRIMARY KEY,
    tenant_id   VARCHAR(36)    NOT NULL,
    channel     VARCHAR(20) NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    config_json TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT uq_channel_config_tenant_channel UNIQUE (tenant_id, channel),
    CONSTRAINT fk_channel_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT ck_channel_config_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP'))
);

CREATE TABLE notification_request (
    id               VARCHAR(36)     NOT NULL PRIMARY KEY,
    tenant_id        VARCHAR(36)     NOT NULL,
    template_code    VARCHAR(100) NOT NULL,
    variables_json   TEXT         NOT NULL,
    recipients_json  TEXT         NOT NULL,
    scheduled_at     TIMESTAMP    NULL,
    idempotency_key  VARCHAR(255) NULL,
    status           VARCHAR(20)  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_by       VARCHAR(36)     NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uq_notification_request_tenant_idem UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT fk_notification_request_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_notification_request_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_notification_request_status CHECK (
        status IN ('PENDING', 'SCHEDULED', 'EXPANDING', 'EXPANDED', 'CANCELLED')
    )
);

CREATE TABLE notification_delivery (
    id                        VARCHAR(36)     NOT NULL PRIMARY KEY,
    notification_request_id  VARCHAR(36)     NOT NULL,
    tenant_id                VARCHAR(36)     NOT NULL,
    channel                  VARCHAR(20)  NOT NULL,
    recipient_id             VARCHAR(255) NOT NULL,
    recipient_address        VARCHAR(500) NOT NULL,
    rendered_subject         VARCHAR(500) NULL,
    rendered_body            TEXT         NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    attempt_count            INT          NOT NULL DEFAULT 0,
    max_attempts             INT          NOT NULL,
    next_attempt_at          TIMESTAMP    NOT NULL,
    claimed_at               TIMESTAMP    NULL,
    worker_id                VARCHAR(100) NULL,
    sent_at                  TIMESTAMP    NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    created_at               TIMESTAMP    NOT NULL,
    updated_at                TIMESTAMP    NOT NULL,
    CONSTRAINT uq_delivery_request_recipient_channel UNIQUE (notification_request_id, recipient_id, channel),
    CONSTRAINT fk_delivery_request FOREIGN KEY (notification_request_id) REFERENCES notification_request (id),
    CONSTRAINT fk_delivery_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT ck_delivery_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')),
    CONSTRAINT ck_delivery_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_delivery_claim ON notification_delivery (tenant_id, status, next_attempt_at);
CREATE INDEX idx_delivery_status_claimed ON notification_delivery (status, claimed_at);

CREATE TABLE delivery_event (
    id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    delivery_id     VARCHAR(36)     NOT NULL,
    attempt_number  INT          NULL,
    attempt_token   VARCHAR(36)     NULL,
    from_status     VARCHAR(20)  NOT NULL,
    to_status       VARCHAR(20)  NOT NULL,
    error_message   VARCHAR(1000) NULL,
    worker_id       VARCHAR(100) NULL,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_delivery_event_attempt_token UNIQUE (attempt_token),
    CONSTRAINT fk_delivery_event_delivery FOREIGN KEY (delivery_id) REFERENCES notification_delivery (id)
);

CREATE INDEX idx_delivery_event_delivery ON delivery_event (delivery_id);
