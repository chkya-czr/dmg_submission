package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** A scheduled (future scheduledAt) send should not produce delivery rows until its time arrives,
 * confirming immediate and scheduled sends share the same expansion path gated purely on time. */
class ScheduledSendExpansionIT extends AbstractIntegrationTest {

    @Test
    void deliveriesAreNotCreatedUntilScheduledAtHasPassed() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());
        String tenantId = tenant.tenantId();

        createTemplate(client, tenantId, "reminder", "IN_APP", null, "Hi {{userName}}", List.of("userName"));

        Instant scheduledAt = Instant.now().plusSeconds(3);
        Map<String, Object> recipient = Map.of("recipientId", "user-1", "channel", "IN_APP", "address", "user-1");
        Map<String, Object> body = Map.of("templateCode", "reminder", "variables", Map.of("userName", "Ada"),
                "scheduledAt", scheduledAt.toString(), "recipients", List.of(recipient));

        ResponseEntity<Map> response = sendNotification(client, tenantId, body, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().get("status")).isEqualTo("SCHEDULED");

        ResponseEntity<Map> immediately = client.getForEntity(
                "/api/tenants/" + tenantId + "/deliveries?recipientId=user-1", Map.class);
        List<Map<String, Object>> immediateContent = (List<Map<String, Object>>) immediately.getBody().get("content");
        assertThat(immediateContent).isEmpty();

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            ResponseEntity<Map> later = client.getForEntity(
                    "/api/tenants/" + tenantId + "/deliveries?recipientId=user-1", Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) later.getBody().get("content");
            assertThat(content).hasSize(1);
            assertThat(content.get(0).get("status")).isEqualTo("SENT");
        });
    }
}
