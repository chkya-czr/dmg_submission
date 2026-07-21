package com.example.notify.dispatch;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robins the starting point through the list of tenants with due deliveries on every poll,
 * instead of always processing them in the same (e.g. created_at) order - so one tenant with a
 * large backlog can't monopolize a dispatch cycle at the expense of others.
 */
@Component
public class TenantFairnessRotator {

    private final AtomicInteger cursor = new AtomicInteger(0);

    public List<String> rotate(List<String> tenantIds) {
        if (tenantIds.isEmpty()) {
            return tenantIds;
        }
        int start = Math.floorMod(cursor.getAndIncrement(), tenantIds.size());
        List<String> rotated = new ArrayList<>(tenantIds.size());
        for (int i = 0; i < tenantIds.size(); i++) {
            rotated.add(tenantIds.get((start + i) % tenantIds.size()));
        }
        return rotated;
    }
}
