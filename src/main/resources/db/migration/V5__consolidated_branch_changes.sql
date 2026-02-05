-- Consolidated Migration: Branch Changes
-- Combines: V5 (artifact graph - later removed), V6 (simplify version control),
--           V7 (drop reason), V8 (event task_execution_id), V9 (artifact status),
--           V11 (todo table)

-- =====================================================
-- RENAME RUN TABLE TO TASK_EXECUTION
-- =====================================================
ALTER TABLE run RENAME TO task_execution;
ALTER
INDEX IF EXISTS idx_run_project_started_at RENAME TO idx_task_execution_project_started_at;

-- =====================================================
-- MODIFY TASK_EXECUTION TABLE
-- =====================================================
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS predecessor_id UUID REFERENCES task_execution(id);
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS message TEXT;
ALTER TABLE task_execution
    ADD COLUMN if NOT EXISTS delta JSONB;
ALTER TABLE task_execution
DROP
COLUMN IF EXISTS reason;

CREATE INDEX if NOT EXISTS idx_task_execution_project ON task_execution (project_id, created_at DESC);
CREATE INDEX if NOT EXISTS idx_task_execution_predecessor ON task_execution (predecessor_id);

-- =====================================================
-- MODIFY ARTIFACT TABLE
-- =====================================================
-- Add new columns
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS file_name VARCHAR (100);
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS description TEXT;
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS prompt TEXT;
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS model VARCHAR (100);
ALTER TABLE artifact
    ADD COLUMN if NOT EXISTS task_execution_id UUID REFERENCES task_execution(id);

-- Copy run_id to task_execution_id before removing
UPDATE artifact
SET task_execution_id = run_id
WHERE run_id IS NOT NULL;

-- Drop old columns
ALTER TABLE artifact DROP COLUMN IF EXISTS run_id;
ALTER TABLE artifact DROP COLUMN IF EXISTS superseded_by_id;

-- Drop old indexes
DROP INDEX if EXISTS idx_artifact_superseded_by_chain;

-- Add new indexes
CREATE INDEX if NOT EXISTS idx_artifact_name ON artifact (project_id, name);
CREATE INDEX if NOT EXISTS idx_artifact_task_execution ON artifact (task_execution_id);

-- Update status constraint to include DELETED
ALTER TABLE artifact DROP CONSTRAINT IF EXISTS artifact_status_check;
ALTER TABLE artifact
    ADD CONSTRAINT artifact_status_check CHECK (status IN ('SCHEDULED', 'CREATED', 'CANCELLED', 'DELETED'));

-- =====================================================
-- UPDATE EVENT TABLE
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
                                                    'todo.list.updated',
                                                    'tool.calling',
                                                    'operator.started',
                                                    'operator.finished',
                                                    'operator.failed',
                                                    'error'
        ));

ALTER TABLE event
    ADD COLUMN if NOT EXISTS task_execution_id UUID REFERENCES task_execution (id);
CREATE INDEX if NOT EXISTS idx_event_task_execution_id ON event (task_execution_id);

-- =====================================================
-- CREATE TODO TABLE
-- =====================================================
CREATE TABLE todo
(
    id                uuid PRIMARY KEY,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    task_execution_id uuid         NOT NULL REFERENCES task_execution (id) ON DELETE CASCADE,
    parent_id         uuid REFERENCES todo (id) ON DELETE CASCADE,
    description       varchar(500) NOT NULL,
    status            varchar(50)  NOT NULL
);

CREATE INDEX idx_todo_task_execution ON todo (task_execution_id);
CREATE INDEX idx_todo_parent ON todo (parent_id);
