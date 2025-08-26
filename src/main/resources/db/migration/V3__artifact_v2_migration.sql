-- V2 migration for ArtifactEntity: drop summary, rename columns
ALTER TABLE artifact
    DROP COLUMN summary;
ALTER TABLE artifact
    RENAME COLUMN inline_json TO content;
ALTER TABLE artifact
    RENAME COLUMN meta_json TO metadata;
ALTER TABLE artifact
    RENAME COLUMN supersedes_id TO superseded_by_id;

-- Update the index on the renamed column
DROP INDEX IF EXISTS idx_artifact_supersedes_chain;
CREATE INDEX idx_artifact_superseded_by_chain ON artifact (superseded_by_id) WHERE superseded_by_id IS NOT NULL;

-- Update the GIN index on the renamed column
DROP INDEX IF EXISTS idx_artifact_inline_gin;
CREATE INDEX idx_artifact_content_gin ON artifact USING gin (content);