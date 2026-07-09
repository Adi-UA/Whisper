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
- **GitHub Actions** — daily cron trigger for rotation.

## Status

🚧 **In design.** See [DESIGN.md](DESIGN.md) for the full architecture and milestones.

## License

MIT
