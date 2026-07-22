# CLAUDE.md

Guidance for Claude Code (or any future agent) working in this repository.

## What this is

A multi-tenant notification service (Spring Boot 3.3, Java 17, Maven). Full context and rationale
is in [README.md](README.md) — read that first for the product framing, API surface, and the
documented scoping assumptions. This file is the quick orientation + conventions doc for making
further changes correctly.

## Running things

- `mvn spring-boot:run` — starts on :8080, H2 file DB at `./data/notify.mv.db`, Flyway-migrated
  automatically. Seeded platform admin: `platform-admin` / `ChangeMe123!`.
- `mvn test` — unit tests only (`*Test.java`, Surefire).
- `mvn verify` — unit + integration tests (`*IT.java`, Failsafe — boots the full app with a real
  HTTP server against an in-memory H2 DB per test class, `application-test.yml` profile).
- Delete `./data/` to reset local state; it's gitignored.

## Package layout (package-by-feature)

`tenant`, `security`, `settings`, `ratelimit`, `template`, `channel`, `notification`, `delivery`,
`dispatch`, `common` (shared: `errors` for the `@RestControllerAdvice` + exception types, `model`
for the shared `Channel` enum, `persistence` for JSON `AttributeConverter`s, `util`, `config`).

## Invariants that must not be broken

1. **Every write to `notification_delivery` or `notification_request` that changes `status` goes
   through a version-guarded conditional `UPDATE`** (see `NotificationDeliveryRepository` /
   `NotificationRequestRepository` — `claim`, `markSent`, `markRetryPending`, `markFailed`,
   `claimForExpansion`, `markExpanded`). Never mutate `status` via a plain JPA entity setter +
   `save()` for these two entities — that bypasses the optimistic-concurrency guard that prevents
   duplicate/lost sends. `ClaimConflictTest` exists specifically to catch a regression here.
2. **Retries reuse the same `notification_delivery` row.** Never insert a second delivery row for
   an existing (request, recipient, channel) tuple — there's a unique constraint backing this up,
   but the application logic (`RequestExpansionScheduler`) also checks
   `existsByNotificationRequestIdAndRecipientIdAndChannel` before inserting, specifically for
   safe re-entrancy if expansion is ever retried.
3. **`@Modifying @Query` repository methods must carry `@Transactional` explicitly.** Spring Data
   does NOT auto-wrap custom modifying queries in a transaction the way it does for inherited
   CRUD methods (`save`/`delete`) — omitting it throws `TransactionRequiredException` at runtime,
   not compile time. This bit us once already (see git history); every existing `@Modifying` method
   already has it — keep that pairing when adding new ones.
4. **`max_attempts` is snapshotted onto `notification_delivery` at creation time** (from the
   tenant's effective limits), not re-read live. Don't "simplify" this to a live lookup — it's
   intentional so a policy change doesn't retroactively affect in-flight deliveries.
5. **Tenant-scoping is not just a role check.** Every tenant-scoped controller method calls
   `TenantAccessGuard.requireAccess(tenantId)` explicitly at the top, in addition to the
   `@PreAuthorize("hasAnyRole(...)")` class-level annotation — the guard is what actually verifies
   a `TENANT_ADMIN`'s own `tenant_id` matches the path, which `@PreAuthorize` alone can't express.
6. **Test naming matters for the build to pick tests up**: unit tests must end in `Test`
   (Surefire), integration tests must end in `IT` (Failsafe, configured in `pom.xml`). A class
   named e.g. `FooTests.java` or `FooIntegrationTest.java` silently won't run in `mvn verify`.

## Conventions

- Entities: plain JPA, no Lombok on entities (explicit getters, private no-arg constructor for
  JPA, a meaningful public constructor for creation). DTOs are Java records.
- Structured data that doesn't need its own table (template `variablesSchema`, request
  `variables`/`recipients`) is stored as a JSON `TEXT` column via a small `AttributeConverter` in
  `common.persistence`, not JSONB/native JSON types — keeps the schema portable.
- Time and randomness are injected (`Clock` bean in `common.config.ClockConfig`, `Random` in
  `WorkerPoolConfig`'s `BackoffCalculator` bean) specifically so logic that depends on them stays
  unit-testable without sleeping or accepting flakiness.
- Errors: throw `common.errors.{BadRequestException,ConflictException,ResourceNotFoundException,
  TenantMismatchException}` from services; `GlobalExceptionHandler` maps them to RFC 7807
  `ProblemDetail` responses. Don't add per-controller try/catch.

## Skills / tooling used during development

No custom Claude Code project skills were needed for this build — it used the built-in
Read/Write/Edit/Bash toolset plus the Plan-mode workflow (an `Explore`-then-`Plan` agent pass to
validate the architecture before any code was written, followed by implementation, a full
`mvn verify` pass, and a live `spring-boot:run` smoke test).
