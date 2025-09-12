-- Consolidated baseline migration
-- This replaces migrations V1 through V7

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

-- Assistant table
CREATE TABLE assistant
(
    id            uuid PRIMARY KEY,
    created_at    timestamptz NOT NULL DEFAULT now(),
    name          text        NOT NULL,
    system_prompt text,
    model         text        NOT NULL,
    params_json   jsonb       NOT NULL
);

-- Thread table
CREATE TABLE thread
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz NOT NULL DEFAULT now(),
    title            text,
    last_activity_at timestamptz NOT NULL
);

-- Message table
CREATE TABLE message
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz NOT NULL DEFAULT now(),
    thread_id        uuid        NOT NULL REFERENCES thread (id) ON DELETE CASCADE,
    author           text        NOT NULL CHECK (author IN ('USER', 'ASSISTANT', 'TOOL', 'SYSTEM')),
    content_json     jsonb       NOT NULL,
    attachments_json jsonb
);

-- Message indexes
CREATE INDEX idx_message_thread_time ON message (thread_id, created_at);
CREATE INDEX idx_message_content_gin ON message USING gin (content_json);

-- Run table
CREATE TABLE run
(
    id           uuid PRIMARY KEY,
    created_at   timestamptz NOT NULL DEFAULT now(),
    thread_id    uuid        NOT NULL REFERENCES thread (id) ON DELETE CASCADE,
    assistant_id uuid        NOT NULL REFERENCES assistant (id),
    status       text        NOT NULL CHECK (status IN
                                             ('QUEUED', 'RUNNING', 'REQUIRES_ACTION', 'COMPLETED',
                                              'FAILED', 'CANCELLED')),
    kind         text        NOT NULL CHECK (kind IN ('FULL', 'PARTIAL')),
    reason       text,
    started_at   timestamptz,
    completed_at timestamptz,
    usage_json   jsonb,
    error        text
);

-- Run indexes
CREATE INDEX idx_run_thread_started_at ON run (thread_id, COALESCE(started_at, completed_at));

-- Artifact table
CREATE TABLE artifact
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz NOT NULL DEFAULT now(),
    thread_id        uuid        NOT NULL REFERENCES thread (id) ON DELETE CASCADE,
    run_id           uuid        REFERENCES run (id) ON DELETE SET NULL,
    kind             text        NOT NULL CHECK (kind IN
                                                 ('STRUCTURED_JSON', 'IMAGE', 'PDF', 'AUDIO',
                                                  'VIDEO')),
    stage            text        NOT NULL CHECK (stage IN ('PLAN', 'ANALYSIS', 'SCRIPT', 'IMAGES', 'DECK')),
    status           text        NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    title            text,
    mime             text,
    storage_key      text,
    content          jsonb,
    metadata         jsonb,
    superseded_by_id uuid REFERENCES artifact (id)
);

-- Artifact indexes
CREATE INDEX idx_artifact_thread_time ON artifact (thread_id, created_at DESC);
CREATE INDEX idx_artifact_stage ON artifact (thread_id, stage, created_at DESC);
CREATE INDEX idx_artifact_superseded_by_chain ON artifact (superseded_by_id) WHERE superseded_by_id IS NOT NULL;
CREATE INDEX idx_artifact_content_gin ON artifact USING gin (content);

-- Event log table
CREATE TABLE event_log
(
    id          uuid PRIMARY KEY,
    created_at  timestamptz          DEFAULT now(),
    thread_id   uuid        NOT NULL REFERENCES thread (id) ON DELETE CASCADE,
    run_id      uuid,
    message_id  uuid,
    event_type  text        NOT NULL CHECK (event_type IN
                                            ('run.created', 'run.updated', 'message.delta',
                                             'message.completed', 'artifact.created', 'error')),
    payload     jsonb       NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    dedupe_key  text
);

-- Event log indexes
CREATE INDEX idx_event_thread_id ON event_log (thread_id, id);
CREATE INDEX idx_event_dedupe ON event_log (thread_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

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

-- Default data
INSERT INTO assistant (id, name, system_prompt, model, params_json)
VALUES ('00000000-0000-0000-0000-000000000001',
        'Default Assistant',
        'You are a helpful AI assistant that specializes in creating marketing content and brand analysis. You can analyze brands, create marketing pitches, generate scripts, and create visual content to support marketing campaigns.',
        'gpt-4',
        '{}'::jsonb);