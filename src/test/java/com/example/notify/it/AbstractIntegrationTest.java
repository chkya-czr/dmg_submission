package com.example.notify.it;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

/**
 * Boots the full application (real HTTP server, real schedulers, real H2-backed persistence) so
 * these tests exercise the actual end-to-end flows rather than mocked slices. Each test uses
 * randomly-suffixed names for tenants/users so it stays independent even if Spring reuses a cached
 * application context (and its H2 database) across test classes with identical configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    protected static final String PLATFORM_ADMIN_USERNAME = "platform-admin";
    protected static final String PLATFORM_ADMIN_PASSWORD = "ChangeMe123!";

    @Autowired
    protected TestRestTemplate restTemplate;

    protected TestRestTemplate asPlatformAdmin() {
        return restTemplate.withBasicAuth(PLATFORM_ADMIN_USERNAME, PLATFORM_ADMIN_PASSWORD);
    }

    protected TestRestTemplate asUser(String username, String password) {
        return restTemplate.withBasicAuth(username, password);
    }

    protected String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected record TenantWithAdmin(String tenantId, String adminUsername, String adminPassword) {
    }

    protected TenantWithAdmin createTenantWithAdmin() {
        Map<String, Object> tenantBody = Map.of("name", unique("tenant"));
        ResponseEntity<Map> tenantResponse =
                asPlatformAdmin().postForEntity("/api/platform/tenants", tenantBody, Map.class);
        String tenantId = (String) tenantResponse.getBody().get("id");

        String adminUsername = unique("admin");
        String adminPassword = "Password123!";
        Map<String, Object> adminBody = Map.of("username", adminUsername, "password", adminPassword);
        asPlatformAdmin().postForEntity("/api/platform/tenants/" + tenantId + "/admins", adminBody, Map.class);

        return new TenantWithAdmin(tenantId, adminUsername, adminPassword);
    }

    protected ResponseEntity<Map> createTemplate(TestRestTemplate client, String tenantId, String code,
                                                  String channel, String subjectTemplate, String bodyTemplate,
                                                  Object variablesSchema) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", code);
        body.put("channel", channel);
        body.put("subjectTemplate", subjectTemplate);
        body.put("bodyTemplate", bodyTemplate);
        body.put("variablesSchema", variablesSchema);
        return client.postForEntity("/api/tenants/" + tenantId + "/templates", body, Map.class);
    }

    protected ResponseEntity<Map> configureChannel(TestRestTemplate client, String tenantId, String channel,
                                                    boolean enabled, Map<String, String> config) {
        Map<String, Object> body = Map.of("enabled", enabled, "config", config);
        return client.exchange("/api/tenants/" + tenantId + "/channels/" + channel, HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders()), Map.class);
    }

    protected ResponseEntity<Map> sendNotification(TestRestTemplate client, String tenantId, Map<String, Object> body,
                                                    String idempotencyKey) {
        HttpHeaders headers = jsonHeaders();
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return client.exchange("/api/tenants/" + tenantId + "/notifications", HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }
}
