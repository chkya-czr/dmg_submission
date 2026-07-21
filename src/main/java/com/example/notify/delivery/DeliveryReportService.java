package com.example.notify.delivery;

import com.example.notify.common.errors.ResourceNotFoundException;
import com.example.notify.common.model.Channel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DeliveryReportService {

    private final NotificationDeliveryRepository deliveryRepository;
    private final DeliveryEventRepository deliveryEventRepository;

    public DeliveryReportService(NotificationDeliveryRepository deliveryRepository,
                                  DeliveryEventRepository deliveryEventRepository) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryEventRepository = deliveryEventRepository;
    }

    public Page<NotificationDelivery> search(String tenantId, DeliveryStatus status, Channel channel,
                                              String recipientId, Instant fromDate, Instant toDate,
                                              Pageable pageable) {
        return deliveryRepository.search(tenantId, status, channel, recipientId, fromDate, toDate, pageable);
    }

    public NotificationDelivery get(String tenantId, String deliveryId) {
        return deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No such delivery: " + deliveryId));
    }

    public java.util.List<DeliveryEvent> getEvents(String deliveryId) {
        return deliveryEventRepository.findByDeliveryIdOrderByCreatedAtAsc(deliveryId);
    }

    /** Delivery counts by status for one notification request, e.g. {"SENT": 8, "PENDING": 2}. */
    public Map<String, Long> countByStatusForRequest(String notificationRequestId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : deliveryRepository.countByStatusForRequest(notificationRequestId)) {
            counts.put(((DeliveryStatus) row[0]).name(), (Long) row[1]);
        }
        return counts;
    }
}
