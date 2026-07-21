package com.example.notify.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantCrudAndRbacIT extends AbstractIntegrationTest {

    @Test
    void platformAdminCanCreateListGetAndUpdateTenants() {
        String name = unique("acme");
        ResponseEntity<Map> created = asPlatformAdmin().postForEntity("/api/platform/tenants", Map.of("name", name), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tenantId = (String) created.getBody().get("id");

        ResponseEntity<Map> fetched = asPlatformAdmin().getForEntity("/api/platform/tenants/" + tenantId, Map.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("name")).isEqualTo(name);
        assertThat(fetched.getBody().get("status")).isEqualTo("ACTIVE");

        ResponseEntity<Map> list = asPlatformAdmin().getForEntity("/api/platform/tenants", Map.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        asPlatformAdmin().patchForObject("/api/platform/tenants/" + tenantId, Map.of("status", "SUSPENDED"), Map.class);
        ResponseEntity<Map> suspended = asPlatformAdmin().getForEntity("/api/platform/tenants/" + tenantId, Map.class);
        assertThat(suspended.getBody().get("status")).isEqualTo("SUSPENDED");
    }

    @Test
    void creatingATenantWithADuplicateNameConflicts() {
        String name = unique("dup-tenant");
        asPlatformAdmin().postForEntity("/api/platform/tenants", Map.of("name", name), Map.class);
        ResponseEntity<Map> second = asPlatformAdmin().postForEntity("/api/platform/tenants", Map.of("name", name), Map.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unauthenticatedRequestsAreRejected() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/platform/tenants", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tenantAdminCannotCallPlatformOnlyEndpoints() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        ResponseEntity<Map> response = asUser(tenant.adminUsername(), tenant.adminPassword())
                .postForEntity("/api/platform/tenants", Map.of("name", unique("nope")), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tenantAdminCanAccessOwnTenantButNotAnotherTenant() {
        TenantWithAdmin tenantA = createTenantWithAdmin();
        TenantWithAdmin tenantB = createTenantWithAdmin();
        var clientA = asUser(tenantA.adminUsername(), tenantA.adminPassword());

        ResponseEntity<Map> ownTemplates = clientA.getForEntity("/api/tenants/" + tenantA.tenantId() + "/templates", Map.class);
        assertThat(ownTemplates.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> otherTenantTemplates =
                clientA.getForEntity("/api/tenants/" + tenantB.tenantId() + "/templates", Map.class);
        assertThat(otherTenantTemplates.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void platformAdminCanAccessAnyTenantsResources() {
        TenantWithAdmin tenant = createTenantWithAdmin();
        ResponseEntity<Map> response =
                asPlatformAdmin().getForEntity("/api/tenants/" + tenant.tenantId() + "/templates", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
