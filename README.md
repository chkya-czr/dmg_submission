# Multi-Tenant Notification Service

A multi-tenant notification service (Spring Boot) supporting email/SMS/push/in-app channels,
tenant-defined templates with variable substitution, immediate and scheduled sends, per-tenant
rate limiting, retries with exponential backoff, duplicate-delivery prevention, and a persisted
audit trail of every delivery state transition.

This was built as a take-home submission from an intentionally open-ended requirement. The
sections below cover how to run it, the assumptions made while scoping it, and the reasoning
behind the architecture.

## Stack

Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation), Flyway, H2 (file-mode), Maven.
No message broker, no containerization — see [Scoping decisions](#scoping-decisions--assumptions)
for why.

## Getting started

### Run it

```bash
mvn spring-boot:run
```

Starts on `http://localhost:8080`, backed by a file-based H2 database at `./data/notify.mv.db`
(created automatically, Flyway-migrated on startup). No external services, Docker, or database
install required.

A bootstrap platform admin is seeded on first startup:

```
username: platform-admin
password: ChangeMe123!
```

This is a placeholder credential for local/grading use only, seeded by
[`PlatformAdminSeeder`](src/main/java/com/example/notify/security/PlatformAdminSeeder.java) rather
than a SQL migration (see [assumption #9](#scoping-decisions--assumptions)).

### Test it

```bash
mvn verify
```

Runs unit tests (`*Test.java`, via Surefire) and integration tests (`*IT.java`, via Failsafe —
full Spring context, real HTTP calls, real H2 persistence, real schedulers) in one pass. Unit
tests alone: `mvn test`.

### Walk through it

With the app running, this end-to-end sequence exercises the core flows (see
[`src/test/java/com/example/notify/it/`](src/test/java/com/example/notify/it/) for the same flows
as automated tests):

```bash
BASE=http://localhost:8080
AUTH="platform-admin:ChangeMe123!"

# 1. Platform admin creates a tenant
TENANT=$(curl -s -u $AUTH -X POST $BASE/api/platform/tenants \
  -H "Content-Type: application/json" -d '{"name":"Acme Corp"}')
TENANT_ID=$(echo "$TENANT" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")

# 2. ...and a tenant admin for it
curl -s -u $AUTH -X POST $BASE/api/platform/tenants/$TENANT_ID/admins \
  -H "Content-Type: application/json" -d '{"username":"acme-admin","password":"AcmePass123!"}'
TADMIN="acme-admin:AcmePass123!"

# 3. Tenant admin creates a template - deliberately mismatched variable first (expect 400)
curl -s -u $TADMIN -X POST $BASE/api/tenants/$TENANT_ID/templates \
  -H "Content-Type: application/json" \
  -d '{"code":"welcome","channel":"EMAIL","subjectTemplate":"Hi {{userName}}","bodyTemplate":"Welcome {{userName}}, code {{otp}}","variablesSchema":["userName"]}'
# -> 400: "Referenced but not declared: [otp]"

# ...fixed:
curl -s -u $TADMIN -X POST $BASE/api/tenants/$TENANT_ID/templates \
  -H "Content-Type: application/json" \
  -d '{"code":"welcome","channel":"EMAIL","subjectTemplate":"Hi {{userName}}","bodyTemplate":"Welcome {{userName}}, code {{otp}}","variablesSchema":["userName","otp"]}'

# 4. Configure the EMAIL channel to fail the first 2 attempts (exercises retry/backoff)
curl -s -u $TADMIN -X PUT $BASE/api/tenants/$TENANT_ID/channels/EMAIL \
  -H "Content-Type: application/json" -d '{"enabled":true,"config":{"failUntilAttempt":"2"}}'

# 5. Send
SEND=$(curl -s -u $TADMIN -X POST $BASE/api/tenants/$TENANT_ID/notifications \
  -H "Content-Type: application/json" \
  -d '{"templateCode":"welcome","variables":{"userName":"Ada","otp":"123456"},"recipients":[{"recipientId":"user-1","channel":"EMAIL","address":"ada@example.com"}]}')
REQ_ID=$(echo "$SEND" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")

# 6. Poll status - watch it go PENDING -> retry -> retry -> SENT (production backoff is 30-60s
#    per attempt by default; see application.yml to speed this up for a demo)
curl -s -u $TADMIN $BASE/api/tenants/$TENANT_ID/notifications/$REQ_ID

# 7. Cross-tenant check - a second tenant's resources are invisible to this admin
TENANT2=$(curl -s -u $AUTH -X POST $BASE/api/platform/tenants -H "Content-Type: application/json" -d '{"name":"Other Co"}')
TENANT2_ID=$(echo "$TENANT2" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")
curl -s -o /dev/null -w "%{http_code}\n" -u $TADMIN $BASE/api/tenants/$TENANT2_ID/templates
# -> 403
```

## API surface

Base path `/api`, HTTP Basic auth. Roles: `PLATFORM_ADMIN`, `TENANT_ADMIN` (scoped to their own
tenant — enforced by [`TenantAccessGuard`](src/main/java/com/example/notify/security/TenantAccessGuard.java)
on every tenant-scoped endpoint, not just role membership).

**Platform admin**
| Method | Path | |
|---|---|---|
| POST/GET | `/platform/tenants` | create / list tenants |
| GET/PATCH | `/platform/tenants/{tenantId}` | get / rename / suspend-activate |
| POST | `/platform/tenants/{tenantId}/admins` | create a tenant admin user |
| GET/PUT | `/platform/settings` | global default rate limit / retry attempts |
| GET/PUT | `/platform/tenants/{tenantId}/limits` | per-tenant overrides (nullable -> falls back to global) |

**Tenant admin** (own tenant; platform admin can act on any tenant)
| Method | Path | |
|---|---|---|
| POST/GET/PUT/DELETE | `/tenants/{tenantId}/templates[/{id}]` | template CRUD (DELETE soft-deletes) |
| GET/PUT | `/tenants/{tenantId}/channels[/{channel}]` | per-channel config (enabled + sender config) |
| GET | `/tenants/{tenantId}/deliveries[/{id}]` | delivery reports; filters: `status`, `channel`, `recipientId`, `fromDate`, `toDate`; `{id}` includes full audit-trail events |

**Send / query**
| Method | Path | |
|---|---|---|
| POST | `/tenants/{tenantId}/notifications` | send (immediate or `scheduledAt`); optional `Idempotency-Key` header |
| GET | `/tenants/{tenantId}/notifications[/{id}]` | history / aggregated delivery-count status |

Errors are [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) `ProblemDetail` responses via a
global `@RestControllerAdvice`. Validation failures include a structured `errors` list.

## Architecture

### Entities

`tenant` · `app_user` (role + tenant scoping) · `global_setting` (singleton defaults) ·
`tenant_limit_policy` (per-tenant nullable overrides) · `notification_template` (per
tenant+code+channel) · `channel_config` (per tenant+channel) · `notification_request` (the
logical "send", holds the raw recipient fan-out spec as JSON until expansion) ·
`notification_delivery` (one row per recipient×channel — the actual work-queue/audit unit) ·
`delivery_event` (immutable state-transition audit log).

All primary keys are app-generated UUIDs (`VARCHAR(36)`); schema avoids vendor-specific SQL so the
same Flyway migrations would run unmodified on Postgres if the DB choice ever changed.

### No message broker — a DB-polling dispatch loop, by design

The requirement explicitly puts "distributed systems / microservices" out of scope and asks for a
single-instance service. Rather than adding Kafka/RabbitMQ (which would mean running a broker
alongside the app — exactly the kind of infrastructure dependency the H2/no-Docker choice is meant
to avoid), the "queue" is just rows in `notification_delivery`, claimed via version-guarded
conditional `UPDATE`s and polled by three `@Scheduled` jobs:

1. **`RequestExpansionScheduler`** — turns a due `notification_request` (immediate, or
   `scheduledAt` in the past) into one `notification_delivery` row per recipient×channel, rendering
   each through the template. A recipient with an unresolvable template/variable gets its own row
   created already `FAILED` with a descriptive audit event — one bad recipient doesn't fail the batch.
2. **`DispatchScheduler`** — the core loop. Each poll: finds tenants with due `PENDING` deliveries,
   round-robins the start point through them via `TenantFairnessRotator` (not FIFO-by-created-at,
   so one tenant's backlog can't starve another), and for each tenant in turn: checks the
   in-memory per-tenant token bucket (`TenantRateLimiter`), then attempts the atomic claim:
   ```sql
   UPDATE notification_delivery SET status='PROCESSING', worker_id=?, claimed_at=?, version=version+1
   WHERE id=? AND status='PENDING' AND version=?
   ```
   A successful claim is handed to a bounded `ExecutorService` (the "worker pool"); the pool permit
   (a `Semaphore` sized to match) is only released once that async task finishes, which is what
   actually bounds concurrent in-flight sends to the pool size.
3. **`StuckDeliverySweeper`** — bulk-reclaims deliveries left in `PROCESSING` past a threshold
   (a worker that crashed or hung), putting them back to `PENDING`. This is the concrete scenario
   the version guard protects against — the poll loop itself is single-threaded and can't race
   with itself, but a stuck worker racing the sweeper's reclaim is a real scenario.

### Duplicate-delivery prevention

Defense in depth, not one mechanism:
- Retries reuse the **same** delivery row (`attempt_count++`) — a retry never inserts a new row.
- Claim / complete / sweep all use the version-guarded conditional `UPDATE` above, so exactly one
  writer ever wins a given transition.
- `notification_request.idempotency_key` (unique per tenant) means a client retrying a `POST` with
  the same `Idempotency-Key` header replays the original request (`200`) instead of creating a
  second one — proven under genuine concurrent races in
  [`DuplicatePreventionIT`](src/test/java/com/example/notify/it/DuplicatePreventionIT.java) (racing
  losers may see a `409` since the check-then-insert isn't perfectly atomic, but never a second
  request).
- `UNIQUE(notification_request_id, recipient_id, channel)` on `notification_delivery` is a DB-level
  backstop against double-expansion.
- [`ClaimConflictTest`](src/test/java/com/example/notify/dispatch/ClaimConflictTest.java) races 8
  threads against the same claim `UPDATE` directly at the repository layer and asserts exactly one wins.

### Retry / backoff

`BackoffCalculator`: `exponential = min(maxDelaySeconds, baseDelaySeconds * 2^(attempt-1))`, then
jittered to `[50%, 100%]` of that value. `max_attempts` is resolved from the tenant's effective
limits **at delivery-creation time** and snapshotted onto the row, so a later policy change doesn't
retroactively alter deliveries already in flight.

### Rate limiting & fairness

Per-tenant in-memory token bucket (`TenantRateLimiter`), checked before every claim attempt — not
persisted (see [assumption #2](#scoping-decisions--assumptions)). Fairness is round-robin at
one-claim-per-tenant-per-pass granularity, not batching, demonstrated in
[`RateLimitAndFairnessIT`](src/test/java/com/example/notify/it/RateLimitAndFairnessIT.java) where a
30-item backlog on one tenant doesn't delay a 1-item send on another.

### Channels

`ChannelSender` is a plain interface (`send(SendRequest) -> SendResult`); the four built-in
implementations are simulated — they log the attempt and can be told (per tenant, per channel, via
`channel_config`) to fail deterministically (`failUntilAttempt=N`, useful for demos/tests), fail
randomly (`failureRate=0..1`), or fail permanently (`alwaysFail=true`). A real integration (SMTP,
Twilio, FCM) is just another bean implementing the same interface for the same channel — see
[assumption #4](#scoping-decisions--assumptions).

### Package layout

Package-by-feature: `tenant`, `security`, `settings`, `ratelimit`, `template`, `channel`,
`notification`, `delivery`, `dispatch`, `common` (shared errors/model/persistence helpers).

## Scoping decisions / assumptions

The requirement is intentionally open-ended; these are the judgment calls made, in one place for
review:

1. **Portable schema, app-generated UUID PKs** everywhere, no vendor-specific SQL — cheap insurance
   even though H2 was the chosen DB.
2. **Rate limiting is per-tenant** (not per-channel) and **in-memory**, not persisted — resets on
   restart. Acceptable for a single instance; avoids a DB round-trip on the dispatch hot path.
3. Retry-attempt limits and rate-limit overrides live in one **combined `tenant_limit_policy`
   table** with nullable override columns falling back to `global_setting`, rather than two
   separate entities.
4. **Channel senders are simulated by default** (no external credentials needed to run or grade);
   the `ChannelSender` interface is the extension point for a real SMTP/Twilio/FCM integration.
5. **PUSH templates are body-only** (no separate title), to limit channel-variant surface area.
6. Template **variable values are shared per send request**, with an optional per-recipient
   override merged on top; per-channel differences are expressed as separate template rows (same
   `code`, different `channel`), not per-channel variable sets.
7. **`Idempotency-Key`** is an HTTP header mapped to `notification_request.idempotency_key`; a
   repeat for the same tenant replays the original resource (`200`) rather than erroring.
8. **Bounded `ThreadPoolExecutor` + `Semaphore`** worker pool (Java 17, not virtual threads) —
   satisfies "bounded worker pools" literally and keeps the build on a widely-available JDK.
9. The **version-guarded claim/complete `UPDATE`s are implemented as real safety nets** (against
   the stuck-delivery sweeper), even though the dispatch poll loop itself is single-threaded and
   can't race with itself today — required by the spec and future-proofs a move to multiple claim
   threads without a schema change.
10. A **default platform admin is seeded from Java at startup** (`PlatformAdminSeeder`), not a SQL
    migration, so its password is hashed with the same `BCryptPasswordEncoder` bean used to verify
    logins — no self-registration flow exists (out of scope).
11. **"Immediate" sends are durably queued synchronously** (`202 Accepted`) but rendered/dispatched
    on the next scheduler poll — worst-case added latency before the first attempt is roughly the
    sum of the expansion and dispatch poll intervals (~1.5s with the defaults in `application.yml`).
    This is an explicit trade-off of a broker-free, DB-polling design, not a bug — see the
    [Kafka discussion](#why-not-kafka) below.
12. **No Kafka/RabbitMQ/SQS.** See below.

### Why not Kafka?

It was considered and explicitly rejected. The assignment's "out of scope" list names "distributed
systems or microservices," and a broker is exactly that category — it would also mean running (or
Testcontainers-ing) a separate service alongside the app, undermining the H2/no-Docker choice made
specifically so the whole thing runs and is gradable with zero external infrastructure. It also
isn't *needed*: every functional requirement (bounded concurrent dispatch, per-tenant fairness,
rate limiting, no duplicate deliveries, retry/backoff) is met within a single JVM by the
claim-based DB-polling design above.

## Known limitations

- Single instance only (by design/requirement) — the dispatch design would need real work
  (`SELECT ... FOR UPDATE SKIP LOCKED` or similar, cross-instance rate limiting) to scale
  horizontally.
- Rate-limiter state is in-memory and resets on restart.
- Dispatch latency floor of roughly the sum of the two poll intervals (default ~1.5s) even for
  "immediate" sends — there's no true synchronous send path.
- Channel senders are simulated; wiring a real provider means adding one `ChannelSender`
  implementation per channel.
- No pagination/rate-limit response headers, no OpenAPI/Swagger UI (kept the surface area to what
  the requirement asked for).

## Testing

- **Unit** (`mvn test`): `BackoffCalculatorTest` (delay bounds/jitter/cap with a seeded `Random`),
  `TemplateRendererTest` (rendering, missing-variable collection, schema validation),
  `TenantRateLimiterTest` (refill/burst/refund with a controllable `Clock`), `TenantAccessGuardTest`
  (tenant-scoping logic in isolation).
- **`ClaimConflictTest`** (`@DataJpaTest`): races 8 threads against the same claim `UPDATE`,
  asserting exactly one wins — the DB-level proof behind the "no duplicate deliveries" claim.
- **Integration** (`mvn verify`, full Spring context + real HTTP + real schedulers):
  `TenantCrudAndRbacIT`, `TemplateValidationIT`, `SendDispatchRetrySuccessIT` (full retry-then-succeed
  flow with audit trail assertions), `RateLimitAndFairnessIT`, `DuplicatePreventionIT` (including
  concurrent racers on the same `Idempotency-Key`), `ScheduledSendExpansionIT`.

## Development artifacts

Per the submission requirements, [`CLAUDE.md`](CLAUDE.md) is included in this repository — it's
the file used to brief Claude Code on this project during development, and also notes the
tools/skills used to build it.
