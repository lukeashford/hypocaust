-- Migration for Artifact Graph with Semantic Anchors
-- Adds support for: branches, commits, artifact relations, semantic anchors, and embeddings

-- =====================================================
-- BRANCH TABLE
-- Tracks branches within a project (like git)
-- =====================================================
CREATE TABLE branch
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    project_id       uuid         NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    name             varchar(255) NOT NULL,
    head_commit_id   uuid,  -- Will be constrained after commit table exists
    parent_branch_id uuid REFERENCES branch (id) ON DELETE SET NULL,
    UNIQUE (project_id, name)
);

CREATE INDEX idx_branch_project ON branch (project_id);

-- =====================================================
-- COMMIT TABLE
-- Immutable snapshots of the artifact graph
-- =====================================================
CREATE TABLE commit
(
    id               uuid PRIMARY KEY,
    created_at       timestamptz NOT NULL DEFAULT now(),
    branch_id        uuid        NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    parent_commit_id uuid REFERENCES commit (id) ON DELETE SET NULL,
    run_id           uuid        NOT NULL REFERENCES run (id) ON DELETE CASCADE,
    task             text        NOT NULL,
    timestamp        timestamptz NOT NULL DEFAULT now(),
    delta            jsonb       NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_commit_branch ON commit (branch_id, created_at DESC);
CREATE INDEX idx_commit_run ON commit (run_id);

-- Add foreign key from branch to commit now that commit exists
ALTER TABLE branch
    ADD CONSTRAINT fk_branch_head_commit
        FOREIGN KEY (head_commit_id) REFERENCES commit (id) ON DELETE SET NULL;

-- =====================================================
-- ARTIFACT RELATION TABLE
-- Tracks derivation and version relationships
-- =====================================================
CREATE TABLE artifact_relation
(
    id                 uuid PRIMARY KEY,
    created_at         timestamptz NOT NULL DEFAULT now(),
    source_artifact_id uuid        NOT NULL REFERENCES artifact (id) ON DELETE CASCADE,
    target_artifact_id uuid        NOT NULL REFERENCES artifact (id) ON DELETE CASCADE,
    relation_type      varchar(50) NOT NULL CHECK (relation_type IN ('DERIVED_FROM', 'SUPERSEDES', 'REFERENCES')),
    UNIQUE (source_artifact_id, target_artifact_id, relation_type)
);

CREATE INDEX idx_artifact_relation_source ON artifact_relation (source_artifact_id);
CREATE INDEX idx_artifact_relation_target ON artifact_relation (target_artifact_id);
CREATE INDEX idx_artifact_relation_type ON artifact_relation (relation_type);

-- =====================================================
-- ANCHOR EMBEDDING TABLE
-- Semantic search on artifact anchors
-- =====================================================
CREATE TABLE anchor_embedding
(
    id          uuid PRIMARY KEY,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    artifact_id uuid         NOT NULL REFERENCES artifact (id) ON DELETE CASCADE,
    embedding   vector(1536) NOT NULL,
    anchor_hash varchar(64)  NOT NULL
);

CREATE INDEX idx_anchor_embedding_artifact ON anchor_embedding (artifact_id);
CREATE INDEX idx_anchor_embedding_hash ON anchor_embedding (anchor_hash);
CREATE INDEX idx_anchor_embedding_vector ON anchor_embedding
    USING ivfflat (embedding vector_cosine_ops);

-- =====================================================
-- ARTIFACT TABLE EXTENSIONS
-- Add semantic anchor and versioning fields
-- =====================================================
ALTER TABLE artifact
    ADD COLUMN anchor_description text;
ALTER TABLE artifact
    ADD COLUMN anchor_role varchar(100);
ALTER TABLE artifact
    ADD COLUMN anchor_tags jsonb DEFAULT '[]'::jsonb;
ALTER TABLE artifact
    ADD COLUMN version integer DEFAULT 1;
ALTER TABLE artifact
    ADD COLUMN derived_from jsonb DEFAULT '[]'::jsonb;
ALTER TABLE artifact
    ADD COLUMN branch_id uuid REFERENCES branch (id) ON DELETE SET NULL;
ALTER TABLE artifact
    ADD COLUMN commit_id uuid REFERENCES commit (id) ON DELETE SET NULL;

-- Index for anchor description full-text search
CREATE INDEX idx_artifact_anchor_description ON artifact USING gin (to_tsvector('english', anchor_description))
    WHERE anchor_description IS NOT NULL;

-- Index for anchor role lookups
CREATE INDEX idx_artifact_anchor_role ON artifact (anchor_role) WHERE anchor_role IS NOT NULL;

-- Index for branch lookups
CREATE INDEX idx_artifact_branch ON artifact (branch_id) WHERE branch_id IS NOT NULL;

-- =====================================================
-- UPDATE EVENT TYPE CHECK
-- Add new event types for branch and commit operations
-- =====================================================
ALTER TABLE event
    DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event
    ADD CONSTRAINT event_type_check CHECK (type IN
                                           ('artifact.scheduled', 'artifact.created', 'artifact.cancelled',
                                            'artifact.updated', 'artifact.superseded',
                                            'run.scheduled', 'run.started', 'run.completed',
                                            'branch.created', 'branch.switched',
                                            'commit.created',
                                            'tool.calling',
                                            'operator.started', 'operator.finished',
                                            'operator.failed',
                                            'error'));
