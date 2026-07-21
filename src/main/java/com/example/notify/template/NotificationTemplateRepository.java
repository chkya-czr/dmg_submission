package com.example.notify.template;

import com.example.notify.common.model.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByTenantIdAndCodeAndChannel(String tenantId, String code, Channel channel);

    boolean existsByTenantIdAndCodeAndChannel(String tenantId, String code, Channel channel);

    Page<NotificationTemplate> findByTenantId(String tenantId, Pageable pageable);

    Optional<NotificationTemplate> findByIdAndTenantId(String id, String tenantId);
}
