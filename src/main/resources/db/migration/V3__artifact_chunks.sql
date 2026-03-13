CREATE TABLE artifact_chunk
(
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    artifact_id uuid         NOT NULL REFERENCES artifact (id) ON DELETE CASCADE,
    project_id  uuid         NOT NULL,
    field_path  text         NOT NULL,
    chunk_index int          NOT NULL,
    char_offset int          NOT NULL,
    text        text         NOT NULL,
    embedding   vector(1536) NOT NULL,
    UNIQUE (artifact_id, field_path, chunk_index)
);

CREATE INDEX idx_artifact_chunk_vector ON artifact_chunk USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_artifact_chunk_project ON artifact_chunk (project_id);
CREATE INDEX idx_artifact_chunk_artifact ON artifact_chunk (artifact_id);
