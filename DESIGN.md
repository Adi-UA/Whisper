# Whisper — Design Document

## Problem

You need a shared daily safe-word with one or more people. Use cases: couple
verification ("prove it's you"), child pickup authentication, duress detection
on phone calls, or a fun daily ritual. Existing solutions: a shared note (no
rotation, no push), a group chat (noisy, scrollable by anyone with phone
access), or nothing.

Whisper generates a two-word phrase from a pre-seeded list, rotates it on a
schedule (daily or weekly), and pushes it as a notification to every member of
a group. No app install required. No cost to run.

## Constraints

- Run on any always-on machine (Windows PC, Linux server, OCI VM) for $0.
- Google OAuth login with an admin-curated email allowlist.
- SQLite on disk as the only database. No external DB dependency.
- Two tech stacks: Java 21 (core service) and Rust (word generator via JNI).
- Deployable to local Kubernetes via Helm for self-hosters.
- Dual delivery: ntfy.sh push (phone) + web UI (browser, post-login).

## Architecture

```
┌───────────────────────────────────────────────────┐
│  Host machine (Windows/Linux/macOS/OCI VM)        │
│                                                   │
│  ┌──────────────┐   JNI    ┌───────────────────┐ │
│  │ Java service  │◄───────►│ Rust word-gen lib  │ │
│  │ (REST API)    │         │ (.dll/.so/.dylib)  │ │
│  └──────┬───────┘         └───────────────────┘ │
│         │                                         │
│         ├── SQLite (whisper.db on disk)            │
│         └── ntfy.sh (push delivery, external)     │
└───────────────────────────────────────────────────┘

┌──────────────────────┐    ┌───────────────────────┐
│ React SPA             │    │ Task Scheduler / cron  │
│ (bundled in the JAR)  │    │ POST /api/rotate daily │
└──────────────────────┘    └───────────────────────┘
```

**Java service** owns: group CRUD, member management, rotation orchestration,
notification dispatch, Google OAuth, rate limiting, Prometheus metrics. Serves
the React SPA from embedded static resources (single JAR, no separate web
server).

**Rust word-gen library** owns: word list loading, sliding-window no-repeat
guarantee, CSPRNG-based selection, phrase assembly. Compiled to a native shared
library and called from Java via JNI. No network hop, no serialization.

**SQLite** stores groups, members, rotation history, and the email allowlist as
a single file on disk. HikariCP connection pool with WAL mode for concurrent
reads.

**ntfy.sh** delivers push notifications. Each member subscribes to a private
topic. Works on Android (native push), iOS (ntfy app), and desktop (browser
notification). Zero cost, zero signup on the recipient side.

## Data Model

```sql
CREATE TABLE allowed_emails (email TEXT PRIMARY KEY);

CREATE TABLE groups (
    id         TEXT PRIMARY KEY,
    name       TEXT NOT NULL,
    pin_hash   TEXT,
    schedule   TEXT NOT NULL DEFAULT 'daily',
    timezone   TEXT NOT NULL DEFAULT 'America/Chicago',
    created_at TEXT NOT NULL
);

CREATE TABLE members (
    id        TEXT PRIMARY KEY,
    group_id  TEXT NOT NULL REFERENCES groups(id),
    name      TEXT NOT NULL,
    channel   TEXT NOT NULL,     -- 'ntfy:<topic>'
    joined_at TEXT NOT NULL
);

CREATE TABLE history (
    group_id   TEXT NOT NULL REFERENCES groups(id),
    phrase     TEXT NOT NULL,
    rotated_at TEXT NOT NULL,
    PRIMARY KEY (group_id, rotated_at)
);
```

## Rotation Flow

1. A scheduler (Task Scheduler on Windows, cron on Linux/macOS) calls
   `POST /api/rotate` once daily.
2. Java service queries all groups. Daily groups rotate every call. Weekly
   groups rotate only on Mondays (evaluated in the group's timezone).
3. For each group: calls Rust word-gen via JNI with the last 100 phrases as
   the exclusion window.
4. Rust selects two words using ChaCha20 CSPRNG seeded deterministically per
   group + rotation count. Rejects any phrase in the window. Returns the phrase.
5. Java writes the phrase to `history`, then fans out notifications: one
   ntfy.sh POST per member's topic.
6. Members receive a push notification on their phone.
7. Members can also see the phrase by logging in to the web UI.

## Authentication

Google OAuth 2.0 via Spring Security (active when `SPRING_PROFILES_ACTIVE=oauth`).
Only Gmail addresses in the `WHISPER_ALLOWED_EMAILS` env var (or the
`allowed_emails` table) can access the API. Everyone else gets rejected at
login.

Without the `oauth` profile, the app runs wide-open (useful for local testing).

## Notification Delivery

Each member chooses a private ntfy topic on join (e.g., `whisper-adi-secret`).
They subscribe to it once in the ntfy app. After that, phrases arrive as push
notifications without any further interaction.

The web UI also shows today's phrase and full history after login.

## Rust Word Generator (JNI Interface)

```rust
pub fn generate_phrase(
    words: &[String],
    history: &[String],
    group_id: &str,
    rotation: u64,
) -> String
```

- Seed: `SHA-256(group_id || rotation_count)` → deterministic, idempotent.
- RNG: ChaCha20Rng from seed. Picks two distinct words, checks against history.
- Bounded retry (100 attempts) prevents infinite loops on small word lists.

## Observability

- Spring Boot Actuator: `/actuator/health`, `/actuator/prometheus`
- Micrometer Prometheus: JVM, HikariCP, HTTP request latency per endpoint
- Custom counters: `whisper.rotations.total`, `whisper.notifications.sent`,
  `whisper.notifications.failed`
- Rate limiting: Bucket4j token bucket on `/api/rotate` (10 RPM default, 429)

## Deployment Options

| Target | Method | Cost |
|--------|--------|------|
| Windows PC (24/7) | NSSM service + Task Scheduler | $0 |
| Linux/macOS | systemd / cron | $0 |
| OCI Always Free VM | cloud-init script provided | $0 |
| Self-hosted (K8s) | `helm install whisper ./chart` | $0 |

## Milestones

**M1: Core end-to-end** ✅
- Rust word-gen library (6 tests, JNI bridge, 431KB native lib)
- Java Spring Boot service (HikariCP, Bucket4j, Actuator + Prometheus)
- SQLite schema, group CRUD, rotation endpoint, ntfy dispatch
- CI: 3-job GitHub Actions (Rust, Java, integration smoke test)
- 15 tests total (6 Rust + 9 Java)

**M2: UI + auth + polish** ✅
- React dashboard (Chakra UI v2, dark mode, Vite 6, TypeScript)
- Google OAuth with email allowlist (profile-gated)
- Sign out, delete groups, remove members
- Hero images in README (desktop + phone notification)
- Full Windows + macOS + Linux setup documentation

**M3: K8s Helm chart** (future)
- Helm chart with CronJob, Deployment, ConfigMap
- `helm template` validation in CI
- Container image push to GHCR
