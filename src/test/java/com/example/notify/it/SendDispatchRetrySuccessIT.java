package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end: send -> deliveries created -> simulated transient failures -> retries with backoff
 * -> eventual success, with the full attempt history visible in the delivery's audit trail.
 */
class SendDispatchRetrySuccessIT extends AbstractIntegrationTest {

    @Test
    void deliveryEventuallySucceedsAfterTwoSimulatedTransientFailures() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());
        String tenantId = tenant.tenantId();

        createTemplate(client, tenantId, "otp", "SMS", null, "Your code is {{code}}", List.of("code"));

        // Fails attempts 1 and 2 (retryable), succeeds on attempt 3.
        configureChannel(client, tenantId, "SMS", true, Map.of("failUntilAttempt", "2"));

        Map<String, Object> recipient = Map.of("recipientId", "user-1", "channel", "SMS", "address", "+15551234567");
        Map<String, Object> sendBody = Map.of("templateCode", "otp", "variables", Map.of("code", "123456"),
                "recipients", List.of(recipient));

        ResponseEntity<Map> sendResponse = sendNotification(client, tenantId, sendBody, null);
        assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String requestId = (String) sendResponse.getBody().get("id");

        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            ResponseEntity<Map> status =
                    client.getForEntity("/api/tenants/" + tenantId + "/notifications/" + requestId, Map.class);
            Map<String, Object> counts = (Map<String, Object>) status.getBody().get("deliveryCountsByStatus");
            assertThat(counts).containsEntry("SENT", 1);
        });

        ResponseEntity<Map> deliveries = client.getForEntity(
                "/api/tenants/" + tenantId + "/deliveries?recipientId=user-1", Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) deliveries.getBody().get("content");
        assertThat(content).hasSize(1);
        String deliveryId = (String) content.get(0).get("id");

        ResponseEntity<Map> deliveryDetail =
                client.getForEntity("/api/tenants/" + tenantId + "/deliveries/" + deliveryId, Map.class);
        assertThat(deliveryDetail.getBody().get("status")).isEqualTo("SENT");
        assertThat(deliveryDetail.getBody().get("attemptCount")).isEqualTo(3);

        List<Map<String, Object>> events = (List<Map<String, Object>>) deliveryDetail.getBody().get("events");
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).containsEntry("toStatus", "PENDING");
        assertThat(events.get(1)).containsEntry("toStatus", "PENDING");
        assertThat(events.get(2)).containsEntry("toStatus", "SENT");
        assertThat(events.get(0).get("attemptNumber")).isEqualTo(1);
        assertThat(events.get(1).get("attemptNumber")).isEqualTo(2);
        assertThat(events.get(2).get("attemptNumber")).isEqualTo(3);
    }
}
