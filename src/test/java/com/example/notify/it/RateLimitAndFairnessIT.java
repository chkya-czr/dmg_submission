package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RateLimitAndFairnessIT extends AbstractIntegrationTest {

    @Test
    void perTenantRateLimitThrottlesThroughputInsteadOfSendingEverythingAtOnce() throws InterruptedException {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());
        String tenantId = tenant.tenantId();

        createTemplate(client, tenantId, "blast", "IN_APP", null, "Hi {{userName}}", List.of("userName"));

        // 2 tokens/sec, burst of 1: only 1 of the 5 recipients can go out immediately, the rest
        // trickle out roughly one every 500ms.
        asPlatformAdmin().exchange("/api/platform/tenants/" + tenantId + "/limits", HttpMethod.PUT,
                new HttpEntity<>(Map.of("rateLimitPerMinute", 120, "burstCapacity", 1), jsonHeaders()), Map.class);

        List<Map<String, Object>> recipients = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            recipients.add(Map.of("recipientId", "user-" + i, "channel", "IN_APP", "address", "user-" + i));
        }
        Map<String, Object> sendBody = Map.of("templateCode", "blast", "variables", Map.of("userName", "Ada"),
                "recipients", recipients);
        ResponseEntity<Map> sendResponse = sendNotification(client, tenantId, sendBody, null);
        String requestId = (String) sendResponse.getBody().get("id");

        Thread.sleep(400); // give the burst allowance a chance to drain, but not enough for a full refill cycle
        int sentSoFar = sentCount(client, tenantId, requestId);
        assertThat(sentSoFar).isLessThan(5);

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> assertThat(sentCount(client, tenantId, requestId)).isEqualTo(5));
    }

    @Test
    void aTenantWithATinyBatchIsNotStarvedByAnotherTenantsLargeBacklog() {
        TenantWithAdmin busyTenant = createTenantWithAdmin();
        TenantWithAdmin quietTenant = createTenantWithAdmin();
        TestRestTemplate busyClient = asUser(busyTenant.adminUsername(), busyTenant.adminPassword());
        TestRestTemplate quietClient = asUser(quietTenant.adminUsername(), quietTenant.adminPassword());

        createTemplate(busyClient, busyTenant.tenantId(), "blast", "IN_APP", null, "Hi {{userName}}", List.of("userName"));
        createTemplate(quietClient, quietTenant.tenantId(), "blast", "IN_APP", null, "Hi {{userName}}", List.of("userName"));

        List<Map<String, Object>> bigBatch = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            bigBatch.add(Map.of("recipientId", "busy-user-" + i, "channel", "IN_APP", "address", "busy-user-" + i));
        }
        Map<String, Object> busyBody = Map.of("templateCode", "blast", "variables", Map.of("userName", "Ada"),
                "recipients", bigBatch);
        sendNotification(busyClient, busyTenant.tenantId(), busyBody, null);

        Map<String, Object> quietRecipient = Map.of("recipientId", "quiet-user", "channel", "IN_APP", "address", "quiet-user");
        Map<String, Object> quietBody = Map.of("templateCode", "blast", "variables", Map.of("userName", "Grace"),
                "recipients", List.of(quietRecipient));
        ResponseEntity<Map> quietSend = sendNotification(quietClient, quietTenant.tenantId(), quietBody, null);
        String quietRequestId = (String) quietSend.getBody().get("id");

        // Fairness round-robins one claim per tenant per pass, so the quiet tenant's single item
        // should complete quickly despite the busy tenant's 30-item backlog dispatching concurrently.
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(150))
                .untilAsserted(() -> assertThat(sentCount(quietClient, quietTenant.tenantId(), quietRequestId)).isEqualTo(1));
    }

    @SuppressWarnings("unchecked")
    private int sentCount(TestRestTemplate client, String tenantId, String requestId) {
        ResponseEntity<Map> status = client.getForEntity("/api/tenants/" + tenantId + "/notifications/" + requestId, Map.class);
        Map<String, Object> counts = (Map<String, Object>) status.getBody().get("deliveryCountsByStatus");
        Object sent = counts.get("SENT");
        return sent == null ? 0 : ((Number) sent).intValue();
    }

    private org.springframework.http.HttpHeaders jsonHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
