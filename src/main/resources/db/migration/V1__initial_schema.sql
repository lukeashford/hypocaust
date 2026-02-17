-- Initial schema

-- Extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE
EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

-- Project table
CREATE TABLE project
(
    id         uuid PRIMARY KEY,
    name       varchar(100) NOT NULL UNIQUE,
    created_at timestamptz  NOT NULL DEFAULT now()
);

-- Task Execution table (formerly run)
CREATE TABLE task_execution
(
    id             uuid PRIMARY KEY,
    created_at     timestamptz NOT NULL DEFAULT now(),
    project_id     uuid        NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    task           text,
    status         text        NOT NULL CHECK (status IN
                                               ('QUEUED', 'RUNNING', 'REQUIRES_ACTION', 'COMPLETED',
                                                'FAILED', 'CANCELLED')),
    started_at     timestamptz,
    completed_at   timestamptz,
    predecessor_id uuid REFERENCES task_execution (id),
    name           varchar(50),
    commit_message text,
    delta          jsonb
);

-- Task Execution indexes
CREATE INDEX idx_task_execution_project_started_at ON task_execution (project_id, coalesce(started_at, completed_at));
CREATE INDEX idx_task_execution_project ON task_execution (project_id, created_at DESC);
CREATE INDEX idx_task_execution_predecessor ON task_execution (predecessor_id);
CREATE UNIQUE INDEX idx_task_execution_project_name ON task_execution (project_id, name);

-- Artifact table
CREATE TABLE artifact
(
    id                uuid PRIMARY KEY,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    project_id        uuid         NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    kind              text         NOT NULL CHECK (kind IN
                                                   ('IMAGE', 'PDF', 'AUDIO',
                                                    'VIDEO', 'TEXT', 'OTHER')),
    status            text         NOT NULL CHECK (status IN
                                                   ('GESTATING', 'CREATED', 'MANIFESTED',
                                                    'CANCELLED',
                                                    'FAILED')),
    title             text,
    storage_key       text,
    inline_content jsonb,
    metadata          jsonb,
    mime_type         text,
    name              varchar(100) NOT NULL,
    description       text,
    task_execution_id uuid REFERENCES task_execution (id)
);

-- Artifact indexes
CREATE INDEX idx_artifact_project_time ON artifact (project_id, created_at DESC);
CREATE INDEX idx_artifact_content_gin ON artifact USING gin (inline_content);
CREATE INDEX idx_artifact_name ON artifact (project_id, name);
CREATE INDEX idx_artifact_task_execution ON artifact (task_execution_id);

-- Event table
CREATE TABLE event
(
    id                uuid PRIMARY KEY,
    created_at        timestamptz DEFAULT now(),
    type              text        NOT NULL CHECK (type IN (
                                                           'artifact.added',
                                                           'artifact.updated',
                                                           'artifact.removed',
                                                           'taskexecution.started',
                                                           'taskexecution.completed',
                                                           'taskexecution.failed',
                                                           'todo.list.updated',
                                                           'task.progress.updated',
                                                           'tool.calling',
                                                           'decomposer.started',
                                                           'decomposer.finished',
                                                           'decomposer.failed',
                                                           'error'
        )),
    payload           jsonb       NOT NULL,
    occurred_at       timestamptz NOT NULL,
    task_execution_id uuid REFERENCES task_execution (id) ON DELETE CASCADE
);

-- Event indexes
CREATE INDEX idx_event_task_execution_id_ordered ON event (task_execution_id, id);

-- Todo table
CREATE TABLE todo
(
    id                uuid PRIMARY KEY,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    task_execution_id uuid         NOT NULL REFERENCES task_execution (id) ON DELETE CASCADE,
    parent_id         uuid REFERENCES todo (id) ON DELETE CASCADE,
    description       varchar(500) NOT NULL,
    status            varchar(50)  NOT NULL
);

-- Todo indexes
CREATE INDEX idx_todo_task_execution ON todo (task_execution_id);
CREATE INDEX idx_todo_parent ON todo (parent_id);

-- Tool embeddings table
CREATE TABLE tool_embeddings
(
    id         uuid PRIMARY KEY,
    created_at timestamptz DEFAULT now(),
    tool_name  varchar(255) UNIQUE NOT NULL,
    embedding  vector(1536)        NOT NULL,
    hash       varchar(64)         NOT NULL
);

-- Tool embeddings index
CREATE INDEX idx_tool_embeddings_vector ON tool_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Model embeddings table
CREATE TABLE model_embeddings
(
    id             uuid PRIMARY KEY,
    created_at     timestamptz                  DEFAULT now(),
    name           varchar(255) UNIQUE NOT NULL,
    embedding      vector(1536)        NOT NULL,
    hash           varchar(64)         NOT NULL,
    owner          VARCHAR(255)        NOT NULL,
    model_id       VARCHAR(255)        NOT NULL,
    description    TEXT                NOT NULL,
    best_practices TEXT                NOT NULL,
    tier           VARCHAR(50)         NOT NULL DEFAULT 'balanced'
);

-- Model embeddings index
CREATE INDEX idx_model_embeddings_vector ON model_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Workflow embeddings table
CREATE TABLE workflow_embeddings
(
    id         uuid PRIMARY KEY,
    created_at timestamptz DEFAULT now(),
    name       varchar(255) UNIQUE NOT NULL,
    embedding  vector(1536)        NOT NULL,
    hash       varchar(64)         NOT NULL,
    text       text                NOT NULL
);

-- Workflow embeddings index
CREATE INDEX idx_workflow_embeddings_vector ON workflow_embeddings USING ivfflat (embedding vector_cosine_ops);
