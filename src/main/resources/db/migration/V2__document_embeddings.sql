-- Create table for platform embeddings
CREATE TABLE if NOT EXISTS platform_embeddings
(
    id
    uuid
    PRIMARY
    KEY,
    created_at
    timestamptz
    DEFAULT
    now
(
),
    name VARCHAR
(
    255
) UNIQUE NOT NULL,
    embedding vector
(
    1536
) NOT NULL,
    hash VARCHAR
(
    64
) NOT NULL,
    text text NOT NULL
    );

-- Vector index for similarity search
CREATE INDEX if NOT EXISTS idx_platform_embeddings_vector
    ON platform_embeddings USING ivfflat (embedding vector_cosine_ops);

-- Create table for workflow embeddings
CREATE TABLE if NOT EXISTS workflow_embeddings
(
    id
    uuid
    PRIMARY
    KEY,
    created_at
    timestamptz
    DEFAULT
    now
(
),
    name VARCHAR
(
    255
) UNIQUE NOT NULL,
    embedding vector
(
    1536
) NOT NULL,
    hash VARCHAR
(
    64
) NOT NULL,
    text text NOT NULL
    );

-- Vector index for similarity search
CREATE INDEX if NOT EXISTS idx_workflow_embeddings_vector
    ON workflow_embeddings USING ivfflat (embedding vector_cosine_ops);
