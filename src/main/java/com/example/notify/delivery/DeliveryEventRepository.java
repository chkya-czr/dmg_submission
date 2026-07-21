package com.example.notify.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, String> {

    List<DeliveryEvent> findByDeliveryIdOrderByCreatedAtAsc(String deliveryId);
}
