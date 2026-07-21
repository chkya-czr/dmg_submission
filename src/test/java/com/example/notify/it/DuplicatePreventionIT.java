package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Duplicate-delivery prevention, exercised from the outside via the HTTP API rather than direct
 * repository races (see {@code ClaimConflictTest} for the raw DB-level race). Covers the
 * Idempotency-Key replay mechanism, including the case where several concurrent requests reuse
 * the same key - the underlying unique constraint on (tenant_id, idempotency_key) is the backstop
 * when the check-then-insert isn't perfectly atomic across truly simultaneous callers.
 */
class DuplicatePreventionIT extends AbstractIntegrationTest {

    @Test
    void repeatingTheSameIdempotencyKeyReplaysTheOriginalRequestInsteadOfCreatingANewOne() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());
        String tenantId = tenant.tenantId();

        createTemplate(client, tenantId, "welcome", "IN_APP", null, "Hi {{userName}}", List.of("userName"));
        Map<String, Object> recipient = Map.of("recipientId", "user-1", "channel", "IN_APP", "address", "user-1");
        Map<String, Object> body = Map.of("templateCode", "welcome", "variables", Map.of("userName", "Ada"),
                "recipients", List.of(recipient));

        String idempotencyKey = unique("idem");
        ResponseEntity<Map> first = sendNotification(client, tenantId, body, idempotencyKey);
        ResponseEntity<Map> replay = sendNotification(client, tenantId, body, idempotencyKey);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().get("id")).isEqualTo(first.getBody().get("id"));

        ResponseEntity<Map> list = client.getForEntity("/api/tenants/" + tenantId + "/deliveries?recipientId=user-1", Map.class);
        List<Map<String, Object>> deliveries = (List<Map<String, Object>>) list.getBody().get("content");
        assertThat(deliveries).hasSize(1);
    }

    @Test
    void concurrentRequestsWithTheSameIdempotencyKeyProduceExactlyOneNotificationRequest() throws Exception {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());
        String tenantId = tenant.tenantId();

        createTemplate(client, tenantId, "race", "IN_APP", null, "Hi {{userName}}", List.of("userName"));
        Map<String, Object> recipient = Map.of("recipientId", "racer", "channel", "IN_APP", "address", "racer");
        Map<String, Object> body = Map.of("templateCode", "race", "variables", Map.of("userName", "Ada"),
                "recipients", List.of(recipient));
        String idempotencyKey = unique("race-idem");

        int racers = 6;
        ExecutorService executor = Executors.newFixedThreadPool(racers);
        List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
        for (int i = 0; i < racers; i++) {
            futures.add(executor.submit(() -> sendNotification(client, tenantId, body, idempotencyKey)));
        }
        Set<String> distinctRequestIds = new java.util.HashSet<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            ResponseEntity<Map> response = future.get();
            // A racing loser may see 409 (lost the unique-constraint race) rather than a clean 200
            // replay, since the check-then-insert isn't perfectly atomic - but it must never create
            // a second underlying request.
            assertThat(response.getStatusCode()).isIn(HttpStatus.ACCEPTED, HttpStatus.OK, HttpStatus.CONFLICT);
            if (response.getStatusCode() != HttpStatus.CONFLICT) {
                distinctRequestIds.add((String) response.getBody().get("id"));
            }
        }
        executor.shutdown();

        assertThat(distinctRequestIds).hasSize(1);

        ResponseEntity<Map> list = client.getForEntity("/api/tenants/" + tenantId + "/deliveries?recipientId=racer", Map.class);
        List<Map<String, Object>> deliveries = (List<Map<String, Object>>) list.getBody().get("content");
        assertThat(deliveries).hasSize(1);
    }
}
