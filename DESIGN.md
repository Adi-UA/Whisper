# Whisper — Design Document

## Problem

You need a shared daily safe-word with one or more people. Use cases: couple
verification ("prove it's you"), child pickup authentication, duress detection
on phone calls, or a fun daily ritual. Existing solutions: a shared note (no
rotation, no push), a group chat (noisy, scrollable by anyone with phone
access), or nothing.

Whisper generates a two-word phrase from a pre-seeded list, rotates it on a
schedule (daily or weekly), and pushes it as a notification to every member of
a group. No app install required. No accounts. No cost to run.

## Constraints

- Host for $0 on free tiers (Fly.io, Turso, Vercel, ntfy.sh, GitHub Actions).
- No OAuth, no email verification. Link-based invite with optional PIN.
- Two tech stacks: Java 21 (core service) and Rust (word generator via JNI).
- Deployable to local Kubernetes via Helm for self-hosters.
- Total cold-start latency under 2 seconds (Fly.io machine wake).

## Architecture

```
┌───────────────────────────────────────────────────┐
│  Fly.io (1 shared-cpu machine, 256 MB)            │
│                                                   │
│  ┌──────────────┐   JNI    ┌───────────────────┐ │
│  │ Java service  │◄───────►│ Rust word-gen lib  │ │
│  │ (REST + cron) │         │ (libwhisper.dylib) │ │
│  └──────┬───────┘         └───────────────────┘ │
│         │                                         │
│         ├── Turso (groups, members, history)       │
│         └── ntfy.sh (push delivery)               │
└───────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────────────────┐
│ Vercel        │         │ GitHub Actions (cron)     │
│ React setup   │         │ curl POST /api/rotate     │
│ UI (static)   │         │ schedule: "0 8 * * *"    │
└──────────────┘         └──────────────────────────┘
```

**Java service** owns: group CRUD, member management, invite links, rotation
orchestration, notification dispatch. Exposes a REST API consumed by the React
UI and the GitHub Actions cron trigger.

**Rust word-gen library** owns: word list loading, sliding-window no-repeat
guarantee, CSPRNG-based selection, phrase assembly. Compiled to a native shared
library (`.so` / `.dylib`) and called from Java via JNI. No network hop, no
serialization overhead.

**Turso** stores groups, members, word lists, and rotation history in SQLite.
Free tier: 9 GB, 500M reads/month.

**ntfy.sh** delivers push notifications. Recipients subscribe to a private
topic (UUID-based). Works on Android (native push), iOS (via ntfy app), and
desktop (browser notification or curl polling). Zero cost, zero signup.

## Data Model

```sql
CREATE TABLE groups (
    id          TEXT PRIMARY KEY,  -- nanoid, 12 chars
    name        TEXT NOT NULL,
    pin_hash    TEXT,              -- bcrypt, nullable (optional PIN)
    schedule    TEXT NOT NULL,     -- 'daily' | 'weekly'
    timezone    TEXT NOT NULL,     -- IANA, e.g. 'America/Chicago'
    word_list   TEXT NOT NULL,     -- 'default' | 'nato' | custom ID
    created_at  TEXT NOT NULL
);

CREATE TABLE members (
    id          TEXT PRIMARY KEY,
    group_id    TEXT NOT NULL REFERENCES groups(id),
    name        TEXT NOT NULL,
    channel     TEXT NOT NULL,     -- 'ntfy:<topic>' | 'email:<addr>'
    joined_at   TEXT NOT NULL
);

CREATE TABLE history (
    group_id    TEXT NOT NULL REFERENCES groups(id),
    phrase      TEXT NOT NULL,
    rotated_at  TEXT NOT NULL,
    PRIMARY KEY (group_id, rotated_at)
);
```

## Rotation Flow

1. GitHub Actions cron fires `POST /api/rotate` at 08:00 UTC daily.
2. Java service queries all groups whose schedule matches today.
3. For each group: calls Rust word-gen via JNI with the group's word list ID
   and the last N phrases (sliding window, N = list size / 2).
4. Rust selects two words using ChaCha20-based CSPRNG seeded per group. Rejects
   any phrase present in the sliding window. Returns the phrase.
5. Java writes the phrase to `history`, then fans out notifications: one
   ntfy.sh POST per member with the phrase as the body.
6. Members receive a push notification: "🤫 velvet falcon".

## Invite Flow

1. Creator hits `POST /api/groups` with name, schedule, timezone, and optional
   PIN. Gets back a group ID.
2. The shareable link is `https://whisper.adi-ua.dev/join/{group_id}`.
3. Recipient opens the link, sees the group name, enters their display name and
   ntfy topic (or picks one auto-generated). If PIN is set, they enter it.
4. `POST /api/groups/{id}/members` validates the PIN (if set), stores the
   member, and subscribes them to the next rotation.

No accounts. No passwords. The link slug is the credential. Optional PIN adds a
second factor for sharing over insecure channels.

## Rust Word Generator (JNI Interface)

```rust
// Exposed to Java via JNI
pub fn generate_phrase(
    word_list: &[String],
    history: &[String],   // last N phrases to avoid
    seed: [u8; 32],       // derived from group_id + rotation count
) -> String
```

Selection algorithm:
1. Split the word list into two pools (adjectives, nouns) or treat as one pool
   and pick two distinct words.
2. Initialize ChaCha20Rng from `seed`.
3. Sample word A from pool. Sample word B from pool (B ≠ A).
4. If "{A} {B}" is in `history`, resample (bounded retry, max 100).
5. Return "{A} {B}".

The seed is deterministic: `SHA-256(group_id || rotation_count)`. This means
the same group on the same day always produces the same phrase (idempotent
retries). But the phrase is unpredictable without knowing the group ID.

## Deployment Options

| Target | Method | Cost |
|--------|--------|------|
| Hosted demo | Fly.io + Turso + Vercel + GitHub Actions cron | $0 |
| Self-hosted (Docker) | `docker compose up` (Java + Rust in one image) | $0 |
| Self-hosted (K8s) | `helm install whisper ./chart` on local cluster | $0 |

The Helm chart defines: a Deployment (Java + Rust sidecar or single image), a
CronJob (rotation trigger), a ConfigMap (word lists), and a Service. Users
provide their own Turso URL or a local SQLite volume.

## Milestones

**M1 (Weekend 1): Core end-to-end**
- Rust: word-gen library with tests, JNI bridge, published as `.so`/`.dylib`.
- Java: Spring Boot service, Turso integration, `/api/groups` CRUD,
  `/api/rotate` endpoint, ntfy.sh dispatch.
- Working demo: create group → rotate → receive push on phone.

**M2 (Weekend 2): UI + polish**
- React setup page (create group, join group, view history).
- Timezone-aware scheduling (store per-group, convert UTC cron to local).
- Custom word list upload (paste or file, stored in Turso).
- Dockerfile (multi-stage: Rust build → Java build → slim runtime image).

**M3 (Weekend 3): K8s + release**
- Helm chart with CronJob, Deployment, ConfigMap.
- CI: build + test + `helm template` validation + container push to GHCR.
- Deploy hosted demo on Fly.io. Link from README.
- Publish: GitHub release, crates.io for the Rust lib (standalone use).

## Open Questions

1. Word list licensing: use Diceware (CC-BY) or build a custom themed list?
2. iOS push: ntfy.sh works on iOS but requires the ntfy app. Acceptable?
3. Rate limiting on `/api/rotate`: GitHub Actions cron is the only caller, but
   the endpoint is public. Add a shared secret header or rely on idempotency?
