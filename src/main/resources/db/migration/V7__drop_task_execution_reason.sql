-- Migration: Remove deprecated reason column from task_execution
-- The reason field is redundant with status (for success) and commitMessage (for errors)

ALTER TABLE task_execution DROP COLUMN IF EXISTS reason;
