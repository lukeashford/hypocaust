-- Create vector extension for PostgreSQL vector operations
CREATE EXTENSION IF NOT EXISTS vector;

-- Create operator_embeddings table for semantic search
CREATE TABLE operator_embeddings
(
    id            BIGSERIAL PRIMARY KEY,
    operator_name VARCHAR(255) UNIQUE NOT NULL,
    embedding     VECTOR(1536)        NOT NULL,
    hash          VARCHAR(64)         NOT NULL,
    created_at    TIMESTAMP DEFAULT NOW()
);

-- Create vector similarity search index using ivfflat
CREATE INDEX idx_operator_embeddings_vector ON operator_embeddings
    USING ivfflat (embedding vector_cosine_ops);