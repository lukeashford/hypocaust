# The Machine

A minimal, future-proof **Claude-style assistant** that turns chat into **versioned artifacts** (
plan → analysis → script → images → deck) with **live streaming** and clean **partial re-runs**.

This README gives you the **architecture principles** so you can quickly orient yourself. It’s
intentionally abstract and provider-agnostic.

---

## Core Principles

* **Thin API, smart backend worker**
  REST for commands, **SSE** for live updates. The web thread never blocks; work runs on a bounded
  executor.

* **Append-only data**
  Messages, runs, and artifacts are appended, not mutated. Revisions create **new artifacts** that *
  *supersede** older ones.

* **Artifacts are first-class**
  The right pane shows artifacts (PLAN/ANALYSIS/SCRIPT/IMAGES/DECK). They can be JSON (inline) or
  files (served via a single download endpoint).

* **Plan → Clarify → Execute**

The assistant can emit a plan, ask missing info, then continue. User changes trigger **selective (
PARTIAL) re-runs**.

* **Reliable reconnection**
  Every UI-relevant change is mirrored into an **event log**. SSE uses that log for gap-free replay
  with `Last-Event-ID`.

* **Keep options open**
  JSONB where formats evolve quickly, UUIDv7 IDs, simple file storage path that can later flip to
  S3, clean path to multi-tenant/auth/RAG without breaking DTOs.

---

## System Overview

**Frontend (React):**

* Left: chat (messages). Right: artifacts grid by stage.
* Initial load: `GET /threads/{id}` (denormalized view).
* Live updates: `GET /threads/{id}/events` (SSE). On reconnect, sends `Last-Event-ID`.

**Backend (Spring Boot):**

* Controllers → Services → Repos.
* One **ExecutorService** runs “runs” (units of assistant work).
* On each state change: persist → write **event\_log** → publish SSE.

**Database (Postgres):**

* Five tables for MVP: `assistant`, `thread`, `message`, `run`, `artifact`.
* Plus `event_log` for replay. JSONB for flexible payloads.

---

## Data Model (MVP)

* **assistant**: model + params for the default agent (one row for MVP).
* **thread**: a conversation.
* **message**: chat bubble with **polymorphic content blocks** (`text`, `markdown`, `tool_call`,
  etc.).
* **run**: one execution over a thread. Status:
  `QUEUED|RUNNING|REQUIRES_ACTION|COMPLETED|FAILED|CANCELLED`. Kind: `FULL|PARTIAL` (for selective
  re-runs).
* **artifact**: assistant outputs (JSON or files). Fields: `kind (STRUCTURED_JSON|IMAGE|PDF|...)`,
  `stage (PLAN|ANALYSIS|SCRIPT|IMAGES|DECK)`, `status`, **`supersedes_id`** to version.
* **event\_log**: append-only stream the UI can replay. Each row becomes one SSE frame (with `seq`
  as the SSE `id:`).

> Indices/constraints enforce valid states and fast queries (timeline, latest-by-stage, supersedes
> chain).

---

## Runtime Flow (Typical)

1. **User** posts a vague prompt (“Analyze a brand and design amarketing
   pitch”).
2. **Run** starts → emits **PLAN** (JSON artifact). If info is missing (e.g., brand), assistant
   sends a clarifying **message** and marks run `REQUIRES_ACTION`.
3. User replies (“Apple”). New **FULL** run produces **ANALYSIS → SCRIPT → IMAGES → (optional) DECK
   ** as separate artifacts.
4. User revises (“Make the main character a girl”). New **PARTIAL** run:

* PLAN v3: shows what changes.
* SCRIPT v2: supersedes v1.
* IMAGES v2..k: only those affected supersede v1s.
* DECK v1: assembled from latest artifacts.

5. Assistant posts “done”.

At every step, the UI updates via SSE events: `run.updated`, `artifact.created`,
`message.completed`.

---

## API Surface (thin, stable)

* `POST /threads` → create a conversation.
* `GET /threads/{id}` → **ThreadView**: thread + messages + artifacts + latest run.
* `POST /runs` → start a run; can include an inline user message (the common path).
* `GET /threads/{id}/events` → **SSE** stream (each frame mirrors an `event_log` row; supports
  `Last-Event-ID`).
* `GET /artifacts/{id}` → download files (images/PDF). JSON artifacts are returned inline via
  `GET /threads/{id}`.

> The client only needs one fetch (`GET /threads/{id}`) and one stream (`/events`). Everything else
> is optional sugar.

---

## Reconnection & Delivery Guarantees

* **Transactional outbox**: entity writes and `event_log` insert occur together.
* **Exactly-once to UI**: SSE `id:` is `event_log.seq`. On reconnect, the client sends
  `Last-Event-ID`; the server replays rows with `seq > lastId`, then switches to live emit.
* **Crash safe**: on server restart, the event log remains the source of truth for replay.

---

## Local Development (minimal)

* **Postgres** required (Flyway applies schema on boot).
* Model provider is pluggable; set your API key(s) as env vars (e.g., `ANTHROPIC_API_KEY` or
  `OPENAI_API_KEY`).
* Files are written under `/var/app/data/{threadId}/{uuid}` (change via properties).
* Start the app: `./gradlew bootRun` (or IDE “Run”).
* Open UI → create a thread → start runs; observe SSE events.

---

## Extending Later (without DTO breaks)

* **Tools**: add `tool`, `tool_binding`, `tool_call` with JSON-Schema validation. Tool I/O already
  fits content blocks/artifact JSON.
* **RAG**: add `corpus/document` + vector store; reference items in messages/artifacts.
* **Auth/multi-tenant**: add `org_id`, `user_id` columns and scope queries.
* **Cloud storage**: switch `GET /artifacts/{id}` to proxy/pre-sign S3; DTOs unchanged.

---

## Local Development with PostgreSQL

The project includes Podman-based PostgreSQL containers for local development. The setup provides:

- PostgreSQL 17.2 database server
- pgAdmin web interface for database management

### Prerequisites

- Podman installed and configured
- Podman machine running (automatically handled by tasks)

### Available Gradle Tasks

All tasks are grouped under "podman-dev" and can be run from IntelliJ GUI or command line:

```bash
./gradlew pods-create     # Create and start PostgreSQL and pgAdmin containers
./gradlew pods-start      # Start existing containers
./gradlew pods-stop       # Stop running containers
./gradlew pods-clear      # Remove all containers and volumes
./gradlew pods-clearPostgres  # Recreate PostgreSQL database
```

### Database Connection Details

- **PostgreSQL**: `localhost:5432`
  - Username: `postgres`
  - Password: `postgres`
  - Database: `the_machine_db`
- **pgAdmin**: `http://localhost:8070`
  - Email: `pgadmin@imp-ag.de`
  - Password: `postgres`

### Usage from IntelliJ

1. Open Gradle tool window (View → Tool Windows → Gradle)
2. Navigate to your project → Tasks → podman-dev
3. Double-click on the desired task

## Development

- The project uses GitHub Actions for continuous integration
- CI automatically builds and tests the application on push to main and pull requests
- Run tests locally with: `./gradlew test`
