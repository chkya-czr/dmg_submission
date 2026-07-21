-- Seeds the singleton global_setting row used as the fallback for tenants that have no
-- tenant_limit_policy override. The bootstrap platform-admin user is NOT seeded here because
-- its password must be BCrypt-hashed at runtime (see PlatformAdminSeeder) rather than
-- hardcoded into a SQL migration.
INSERT INTO global_setting (id, default_rate_limit_per_minute, default_burst_capacity, default_max_retry_attempts, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 60, 10, 5, CURRENT_TIMESTAMP);
