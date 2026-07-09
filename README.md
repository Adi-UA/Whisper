# Whisper

A shared daily safe-word for your people. Whisper generates a two-word phrase,
rotates it on a schedule (daily or weekly), and pushes it as a notification to
every member of a group. No app install. No accounts. No cost to run.

## Use Cases

- Couple/family verification ("prove it's you on the phone")
- Child pickup authentication (only go with someone who knows today's word)
- Fun daily shared ritual between partners or friends

## Tech Stack

- **Java 21** (Spring Boot, virtual threads) — core service, REST API, scheduling
- **Rust** — word generator, called from Java via JNI. CSPRNG selection, no-repeat guarantee.
- **Turso** — SQLite on the edge. Groups, members, rotation history.
- **ntfy.sh** — free push notifications, zero signup for recipients.
- **React + TypeScript** — setup UI (create group, join, view history).
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
