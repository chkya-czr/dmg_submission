package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateValidationIT extends AbstractIntegrationTest {

    @Test
    void createsAnEmailTemplateWhenSchemaMatchesTemplateText() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());

        ResponseEntity<Map> response = createTemplate(client, tenant.tenantId(), "welcome", "EMAIL",
                "Welcome {{userName}}", "Hello {{userName}}, your order {{orderId}} shipped.",
                List.of("userName", "orderId"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("active")).isEqualTo(true);
    }

    @Test
    void rejectsEmailTemplateWithoutASubject() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());

        ResponseEntity<Map> response = createTemplate(client, tenant.tenantId(), "no-subject", "EMAIL",
                null, "Hello {{userName}}", List.of("userName"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTemplateWhenSchemaDoesNotMatchReferencedVariables() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());

        ResponseEntity<Map> response = createTemplate(client, tenant.tenantId(), "mismatch", "SMS",
                null, "Hello {{userName}}, code {{otp}}", List.of("userName"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody().get("detail"))).contains("otp");
    }

    @Test
    void rejectsDuplicateCodeAndChannelForTheSameTenant() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());

        createTemplate(client, tenant.tenantId(), "dup", "SMS", null, "Hello {{userName}}", List.of("userName"));
        ResponseEntity<Map> second = createTemplate(client, tenant.tenantId(), "dup", "SMS", null,
                "Hi {{userName}}", List.of("userName"));

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void allowsTheSameCodeOnADifferentChannel() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        TestRestTemplate client = asUser(tenant.adminUsername(), tenant.adminPassword());

        ResponseEntity<Map> smsResponse = createTemplate(client, tenant.tenantId(), "welcome", "SMS", null,
                "Hi {{userName}}", List.of("userName"));
        ResponseEntity<Map> pushResponse = createTemplate(client, tenant.tenantId(), "welcome", "PUSH", null,
                "Hi {{userName}}", List.of("userName"));

        assertThat(smsResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(pushResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
