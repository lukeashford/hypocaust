# The Machine

A minimal, future-proof **Claude-style assistant** that turns chat into **versioned artifacts** (
plan → analysis → script → images → deck) with **live streaming** and clean **partial re-runs**.

This README gives you the **architecture principles** and **implementation patterns** so you can
quickly orient yourself and contribute effectively.

## Technology Stack

**Core Framework:** Java 21 + Spring Boot 3.5.0 with immutable-first design (`val` everywhere,
builder patterns, `@RequiredArgsConstructor` for DI)

**AI Integration:** Spring AI 1.0.0 (provider-agnostic, currently OpenAI configured, ready for
Anthropic/others)

**Data Layer:** PostgreSQL with RFC 9562-compliant **UUID v7 IDs** for timestamp-ordered
performance + **JSONB with GIN indexes** for flexible schemas

**Mapping & Serialization:** MapStruct for clean entity-DTO boundaries + Jackson with non-null
serialization, ISO dates, UTC timezone standardization

**Development Infrastructure:** Podman-based local development with PostgreSQL 17.2 + pgAdmin,
integrated Gradle lifecycle management

**Database Migrations:** Flyway with validation-first approach (validates schema against migrations
on boot)

---

## Core Principles

* **Thin API, smart backend worker**  
  REST for commands, **SSE** for live updates. The web thread never blocks; work runs on a bounded
  executor.

* **Append-only event sourcing**  
  Messages, runs, and artifacts are appended, not mutated. **Event log** uses `bigserial`
  sequencing + `dedupe_key` for idempotent processing.

* **Artifacts are first-class**  
  The right pane shows artifacts (PLAN/ANALYSIS/SCRIPT/IMAGES/DECK). JSON (inline) or files (single
  download endpoint `/artifacts/{id}`).

* **Plan → Clarify → Execute**  
  Assistant emits plans, asks missing info, then continues. User changes trigger **selective (
  PARTIAL) re-runs**.

* **Reliable reconnection**  
  Every UI change mirrored into **transactional outbox** (`event_log`). SSE uses `Last-Event-ID` for
  gap-free replay.

* **Keep options open**  
  JSONB where formats evolve, UUID v7 IDs, simple file storage path (ready for S3), clean
  multi-tenant/auth/RAG extension path.

---

## Data Architecture Patterns

### UUID v7 Implementation

Custom **RFC 9562-compliant generator** creates timestamp-ordered IDs that improve database
performance vs random UUIDs. Embedded in all primary keys for natural chronological sorting.

### PostgreSQL-Specific Features

- **Extensions**: `uuid-ossp` for additional UUID functions
- **JSONB + GIN Indexes**: Fast search on `content_json`, `inline_json`, `params_json`
- **Composite Indexes**: Timeline queries (`thread_id, created_at`), latest-by-stage lookups
- **Database-level Constraints**: Enum validation, cascading deletes, referential integrity

### Strategic Indexing

```sql
-- Timeline performance
CREATE INDEX idx_message_thread_time ON message (thread_id, created_at);
-- Artifact versioning chain
CREATE INDEX idx_artifact_supersedes_chain ON artifact (supersedes_id) WHERE supersedes_id IS NOT NULL;
-- Event replay
CREATE INDEX idx_event_thread_seq ON event_log (thread_id, seq);
```

---

## System Overview

**Frontend (React):**

* Left: chat (messages). Right: artifacts grid by stage.
* Initial load: `GET /threads/{id}` (denormalized view).
* Live updates: `GET /threads/{id}/events` (SSE). On reconnect, sends `Last-Event-ID`.

**Backend (Spring Boot):**

* Controllers → Services → Repos with **clean architecture separation**.
* One **ExecutorService** runs "runs" (units of assistant work).
* On each state change: persist → write **event_log** → publish SSE.
* **Auto-seeding**: Creates default Claude 3.7 assistant on first startup.

**Database (PostgreSQL):**

* Five tables for MVP: `assistant`, `thread`, `message`, `run`, `artifact`.
* Plus `event_log` for replay. JSONB for flexible, evolving payloads.

---

## Data Model (MVP)

* **assistant**: AI model + parameters stored as JSONB. Pluggable provider strategy (OpenAI
  configured, supports Anthropic/others).
* **thread**: A conversation with `last_activity_at` for timeline queries.
* **message**: Chat bubbles with **polymorphic content blocks** (`text`, `markdown`, `tool_call`)
  stored as JSONB.
* **run**: One execution over a thread. Status:
  `QUEUED|RUNNING|REQUIRES_ACTION|COMPLETED|FAILED|CANCELLED`. Kind: `FULL|PARTIAL`.
* **artifact**: Assistant outputs with versioning. Fields: `kind`, `stage`, `status`, *
  *`supersedes_id`** for version chains.
* **event_log**: Append-only stream with `seq` (bigserial) as SSE event ID. **Deduplication** via
  `dedupe_key`.

> Database constraints enforce valid states. Strategic indexes optimize timeline queries, artifact
> chains, and event replay.

---

## Bootstrap & Configuration Patterns

### Environment-Driven Configuration

```yaml
# Database credentials via env vars (Docker-friendly)
datasource:
  username: ${DB_USERNAME:postgres}
  password: ${DB_PASSWORD:postgres}

# AI provider API keys
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
```

### Auto-Seeding Strategy

- **BootstrapConfig** creates default assistant on `ApplicationReadyEvent`
- Uses custom UUID v7 generator for deterministic startup
- JSONB parameters: `{"temperature": 0.4, "maxOutputTokens": 2048}`
- Fail-fast error handling with structured logging

### Jackson Customization

- **Non-null serialization**: Cleaner JSON responses
- **ISO date formats**: Frontend-friendly timestamps
- **Lenient deserialization**: `fail-on-unknown-properties: false`
- **UTC timezone**: All timestamps normalized for global consistency

---

## Runtime Flow (Typical)

1. **User** posts a vague prompt ("Analyze a brand and design a marketing pitch").
2. **Run** starts → emits **PLAN** (JSON artifact). If info missing, assistant sends clarifying *
   *message** and marks run `REQUIRES_ACTION`.
3. User replies ("Apple"). New **FULL** run produces **ANALYSIS → SCRIPT → IMAGES → (optional) DECK
   ** as separate artifacts.
4. User revises ("Make the main character a girl"). New **PARTIAL** run:

* PLAN v3: shows what changes
* SCRIPT v2: supersedes v1
* IMAGES v2..k: only affected ones supersede v1s
* DECK v1: assembled from latest artifacts

5. Assistant posts "done".

At every step, UI updates via SSE events: `run.updated`, `artifact.created`, `message.completed`.

---

## Event Sourcing Implementation

### Transactional Outbox Pattern

Entity writes and `event_log` inserts occur in same transaction. Guarantees consistency between
business state and event stream.

### Event Types Taxonomy

`run.created`, `run.updated`, `message.delta`, `message.completed`, `artifact.created`, `error`

### Deduplication Strategy

Uses `dedupe_key` field for idempotent event processing. Critical for reliable partial re-runs and
reconnection scenarios.

### Serial Sequencing

PostgreSQL `bigserial` on `seq` field guarantees ordered event delivery. SSE `id:` maps directly to
`seq` value.

---

## API Surface (thin, stable)

* `POST /threads` → create a conversation
* `GET /threads/{id}` → **ThreadViewDTO**: thread + messages + artifacts + latest run
* `POST /runs` → start a run; can include inline user message (common path)
* `GET /threads/{id}/events` → **SSE** stream (each frame mirrors `event_log` row; supports
  `Last-Event-ID`)
* `GET /artifacts/{id}` → download files (images/PDF). JSON artifacts returned inline via
  `GET /threads/{id}`

> Client needs only one fetch (`GET /threads/{id}`) and one stream (`/events`). Everything else is
> optional sugar.

---

## Reconnection & Delivery Guarantees

* **Transactional outbox**: Entity writes and `event_log` insert occur atomically
* **Exactly-once to UI**: SSE `id:` is `event_log.seq`. On reconnect, client sends `Last-Event-ID`;
  server replays `seq > lastId`, then switches to live emit
* **Crash safe**: Event log remains source of truth for replay after server restart
* **Gap-free delivery**: Sequential `seq` values ensure no missed events during reconnection

---

## Development Infrastructure

### Container-First Development

Complete local stack with **PostgreSQL + pgAdmin** via Podman. Custom Gradle tasks integrate
container lifecycle into build process.

### Available Gradle Tasks

```bash
./gradlew pods-create     # Create and start containers (auto-handles Podman machine)
./gradlew pods-start      # Start existing containers  
./gradlew pods-stop       # Stop running containers
./gradlew pods-clear      # Remove all containers and volumes
./gradlew pods-clearPostgres  # Recreate database only
```

### Database Connection Strategy

- **Non-standard port (7888)**: Avoids conflicts with existing PostgreSQL installations
- **Environment-driven credentials**: `DB_USERNAME`, `DB_PASSWORD` env vars
- **pgAdmin**: `http://localhost:7889` for database management

### Validation-First Boot Process

Hibernate validates schema against Flyway migrations on startup. Fail-fast approach catches schema
drift early.

---

## Current State vs. Future Vision

### MVP Scope (Implemented)

- ✅ **Thread creation and viewing**: Basic conversation lifecycle
- ✅ **Data model foundation**: All tables, constraints, indexes
- ✅ **Event log infrastructure**: Transactional outbox pattern
- ✅ **Clean architecture**: Controllers, Services, DTOs with MapStruct
- ✅ **Development tooling**: Podman integration, auto-seeding

### Planned Extensions

- 🔄 **Run execution engine**: Actual AI processing and artifact generation
- 🔄 **SSE streaming**: Event streaming endpoint implementation
- 🔄 **Additional services**: RunService, ArtifactService, EventService
- 🔄 **File artifact handling**: Storage, serving, and management

### Extension Paths (without DTO breaks)

* **Tools**: Add `tool`, `tool_binding`, `tool_call` with JSON-Schema validation
* **RAG**: Add `corpus/document` + vector store; reference in messages/artifacts
* **Auth/multi-tenant**: Add `org_id`, `user_id` columns and scope queries
* **Cloud storage**: Switch `GET /artifacts/{id}` to proxy/pre-sign S3

---

## Architectural Decisions

### Immutable-First Design

- **Kotlin-style `val`** + **Lombok `val`** throughout Java codebase
- **Builder patterns** over setters for object construction
- **`@RequiredArgsConstructor`** for clean dependency injection
- **Java records** + **Kotlin `@JvmRecord`** for DTOs

### Clean Architecture Patterns

- **MapStruct boundaries**: Entity-DTO separation without boilerplate
- **Transaction boundaries**: Clear read-only vs. write demarcation
- **Fail-fast validation**: Database constraints + application-level checks
- **Minimal annotations**: Only what's required now, no speculative additions

### Performance Considerations

- **UUID v7**: Natural database ordering, better index performance than UUID v4
- **Strategic JSONB usage**: Flexible where schemas evolve, structured where performance matters
- **Composite indexes**: Optimized for expected query patterns (timeline, latest-by-stage)

---

## Quick Start

### Prerequisites

- Java 21, Podman installed
- Set `OPENAI_API_KEY` environment variable

### Local Development

```bash
# Start database
./gradlew pods-create

# Run application  
./gradlew bootRun

# Test endpoints
curl -X POST http://localhost:8080/threads
curl -X GET http://localhost:8080/threads/{id}

# Run tests
./gradlew test
```

Database available at `localhost:7888`, pgAdmin at `http://localhost:8070`.

---

## Contributing

- Follow **Spring Boot conventions** and **immutable-first patterns**
- Use **`val`** wherever suitable, prefer **builders over setters**
- Keep **class annotations minimal** and purpose-driven
- Update this README when architectural patterns change
- Run full test suite before submitting PRs: `./gradlew test`