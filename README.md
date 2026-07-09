# Whisper

A shared daily safe-word for your people. Whisper generates a two-word phrase,
rotates it on a schedule (daily or weekly), and pushes it as a notification to
every member of a group. No app install. No accounts. No cost to run.

## Use Cases

- Couple/family verification ("prove it's you on the phone")
- Child pickup authentication (only go with someone who knows today's word)
- Fun daily shared ritual between partners or friends

## How It Works

1. You add approved Gmail addresses to the allowlist.
2. Approved users log in with Google, create or join groups.
3. Each day (or week), a new two-word phrase is generated and pushed to every
   group member's phone via ntfy.sh.
4. Members can also see the current phrase by logging in to the web UI.

## Tech Stack

- **Java 21** (Spring Boot, Spring Security OAuth2, virtual threads) — core service, REST API, Google login
- **Rust** — word generator, called from Java via JNI. CSPRNG selection, no-repeat guarantee.
- **Turso** — hosted SQLite. Groups, members, rotation history, email allowlist.
- **ntfy.sh** — free push notifications to phone, zero signup for recipients.
- **React + TypeScript** — web UI (login, create/join groups, view today's phrase + history).
- **Kubernetes** — Helm chart for self-hosters. Local dev via OrbStack/minikube.
- **OCI Always Free** — Production hosting on Oracle Cloud ARM VM ($0).

## Running Locally

Whisper runs as a single Java process calling a native Rust library. No Docker
or Kubernetes required.

### Prerequisites

- Java 21+
- Rust toolchain (`rustup` or `mise use rust`)

### Build

```bash
# Build the Rust word generator (produces a native shared library)
cd wordgen
cargo build --release
cd ..

# Build the Java service
cd service
./mvnw package -DskipTests
cd ..
```

The Rust build produces a platform-specific library:

| OS | File | Path |
|----|------|------|
| macOS | `libwhisper_wordgen.dylib` | `wordgen/target/release/` |
| Linux | `libwhisper_wordgen.so` | `wordgen/target/release/` |
| Windows | `whisper_wordgen.dll` | `wordgen\target\release\` |

### Run

Point Java at the native library and start the service:

```bash
java -Djava.library.path=./wordgen/target/release \
     -jar service/target/whisper-service-0.0.1-SNAPSHOT.jar
```

On Windows (PowerShell):

```powershell
java -D"java.library.path=.\wordgen\target\release" `
     -jar service\target\whisper-service-0.0.1-SNAPSHOT.jar
```

The service starts on `http://localhost:8080`. Trigger a rotation manually:

```bash
curl -X POST http://localhost:8080/api/rotate
```

Or schedule it daily via cron (Linux/macOS), Task Scheduler (Windows), or a
systemd timer (OCI VM).

## Status

🚧 **In development.** See [DESIGN.md](DESIGN.md) for the full architecture and milestones.

## License

MIT
