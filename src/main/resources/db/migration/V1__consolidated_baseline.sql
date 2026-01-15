-- Consolidated baseline migration for simplified architecture
-- Only persists: embeddings (operators, workflows, platforms) and artifacts

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

-- Project table (lightweight, just tracks project IDs for grouping)
CREATE TABLE project
(
    id         uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Run table
CREATE TABLE run
(
    id           uuid PRIMARY KEY,
    created_at   timestamptz NOT NULL DEFAULT now(),
    project_id   uuid        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    task         text,
    status       text        NOT NULL CHECK (status IN
                                             ('QUEUED', 'RUNNING', 'REQUIRES_ACTION', 'COMPLETED',
                                              'FAILED', 'CANCELLED')),
    reason       text,
    started_at   timestamptz,
    completed_at timestamptz
);

-- Run indexes
CREATE INDEX idx_run_project_started_at ON run (project_id, COALESCE(started_at, completed_at));

-- Artifact table
CREATE TABLE artifact
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz NOT NULL DEFAULT now(),
    project_id       uuid        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    run_id           uuid        REFERENCES run (id) ON DELETE SET NULL,
    kind             text        NOT NULL CHECK (kind IN
                                                 ('STRUCTURED_JSON', 'IMAGE', 'PDF', 'AUDIO',
                                                  'VIDEO')),
    status           text        NOT NULL CHECK (status IN ('SCHEDULED', 'CREATED', 'CANCELLED')),
    title            text,
    mime             text,
    storage_key      text,
    content          jsonb,
    metadata         jsonb,
    superseded_by_id uuid REFERENCES artifact (id)
);

-- Artifact indexes
CREATE INDEX idx_artifact_project_time ON artifact (project_id, created_at DESC);
CREATE INDEX idx_artifact_superseded_by_chain ON artifact (superseded_by_id) WHERE superseded_by_id IS NOT NULL;
CREATE INDEX idx_artifact_content_gin ON artifact USING gin (content);

-- Event table
CREATE TABLE event
(
    id          uuid PRIMARY KEY,
    created_at  timestamptz DEFAULT now(),
    project_id  uuid        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    project_seq uuid        NOT NULL,
    type        text        NOT NULL CHECK (type IN
                                            ('artifact.scheduled', 'artifact.created',
                                             'artifact.cancelled',
                                             'run.scheduled', 'run.started', 'run.completed',
                                             'tool.calling',
                                             'error')),
    payload     jsonb       NOT NULL,
    occurred_at timestamptz NOT NULL,
    dedupe_key  text
);

-- Event indexes
CREATE INDEX idx_event_project_id ON event (project_id, id);
CREATE INDEX idx_event_dedupe ON event (project_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

-- Operator embeddings table
CREATE TABLE operator_embeddings
(
    id            uuid PRIMARY KEY,
    created_at    timestamptz DEFAULT now(),
    operator_name VARCHAR(255) UNIQUE NOT NULL,
    embedding     vector(1536)        NOT NULL,
    hash          VARCHAR(64)         NOT NULL
);

-- Operator embeddings index
CREATE INDEX idx_operator_embeddings_vector ON operator_embeddings
    USING ivfflat (embedding vector_cosine_ops);
