-- Migration: Simplify Artifact Version Control
-- Removes: branches, commits, embeddings, relations
-- Renames: run -> task_execution
-- Adds: predecessor_id, message, delta to task_execution
-- Adds: fileName, description, prompt, model, deleted, task_execution_id to artifact

-- =====================================================
-- DROP BRANCH AND COMMIT RELATED TABLES
-- =====================================================
DROP TABLE IF EXISTS anchor_embedding;
DROP TABLE IF EXISTS artifact_relation;
DROP TABLE IF EXISTS commit CASCADE;
DROP TABLE IF EXISTS branch CASCADE;

-- =====================================================
-- RENAME RUN TABLE TO TASK_EXECUTION
-- =====================================================
ALTER TABLE run RENAME TO task_execution;

-- Rename the index as well
ALTER INDEX IF EXISTS idx_run_project_started_at RENAME TO idx_task_execution_project_started_at;

-- =====================================================
-- MODIFY TASK_EXECUTION TABLE (formerly run)
-- =====================================================
ALTER TABLE task_execution
    ADD COLUMN IF NOT EXISTS predecessor_id UUID REFERENCES task_execution(id);
ALTER TABLE task_execution
    ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE task_execution
    ADD COLUMN IF NOT EXISTS delta JSONB;

-- Add indexes for predecessor chain traversal
CREATE INDEX IF NOT EXISTS idx_task_execution_project ON task_execution (project_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_execution_predecessor ON task_execution (predecessor_id);

-- =====================================================
-- MODIFY ARTIFACT TABLE
-- =====================================================
-- Add new columns
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(100);
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS prompt TEXT;
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS model VARCHAR(100);
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE artifact
    ADD COLUMN IF NOT EXISTS task_execution_id UUID REFERENCES task_execution(id);

-- Copy run_id to task_execution_id before removing (run table was renamed)
UPDATE artifact SET task_execution_id = run_id WHERE run_id IS NOT NULL;

-- Drop old columns
ALTER TABLE artifact DROP COLUMN IF EXISTS version;
ALTER TABLE artifact DROP COLUMN IF EXISTS commit_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_description;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_role;
ALTER TABLE artifact DROP COLUMN IF EXISTS anchor_tags;
ALTER TABLE artifact DROP COLUMN IF EXISTS superseded_by_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS branch_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS derived_from;
ALTER TABLE artifact DROP COLUMN IF EXISTS run_id;

-- Drop old indexes
DROP INDEX IF EXISTS idx_artifact_anchor_description;
DROP INDEX IF EXISTS idx_artifact_anchor_role;
DROP INDEX IF EXISTS idx_artifact_branch;
DROP INDEX IF EXISTS idx_artifact_superseded_by_chain;

-- Add new indexes
CREATE INDEX IF NOT EXISTS idx_artifact_name ON artifact (project_id, name);
CREATE INDEX IF NOT EXISTS idx_artifact_task_execution ON artifact (task_execution_id);

-- =====================================================
-- UPDATE EVENT TYPE CONSTRAINT
-- =====================================================
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;
ALTER TABLE event
    ADD CONSTRAINT event_type_check CHECK (type IN (
        'artifact.added',
        'artifact.updated',
        'artifact.removed',
        'taskexecution.started',
        'taskexecution.completed',
        'taskexecution.failed',
        'task.progress.updated',
        'tool.calling',
        'operator.started',
        'operator.finished',
        'operator.failed',
        'error'
    ));
