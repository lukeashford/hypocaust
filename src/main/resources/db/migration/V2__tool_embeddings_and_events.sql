-- Tool embeddings table (replaces operator_embeddings)
CREATE TABLE tool_embeddings
(
    id         uuid PRIMARY KEY,
    created_at timestamptz DEFAULT now(),
    tool_name  varchar(255) UNIQUE NOT NULL,
    embedding  vector(1536)        NOT NULL,
    hash       varchar(64)         NOT NULL
);

CREATE INDEX idx_tool_embeddings_vector ON tool_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Drop old operator_embeddings table (replaced by tool_embeddings)
DROP TABLE IF EXISTS operator_embeddings;

-- Update event type CHECK constraint to include decomposer events
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event ADD CONSTRAINT event_type_check CHECK (type IN (
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
));
