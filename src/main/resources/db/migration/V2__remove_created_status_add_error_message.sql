-- Remove CREATED artifact status and add error_message column

-- Update any existing CREATED artifacts to MANIFESTED (they should have been materialized)
UPDATE artifact SET status = 'MANIFESTED' WHERE status = 'CREATED';

-- Drop and recreate the status check constraint without CREATED
ALTER TABLE artifact DROP CONSTRAINT IF EXISTS artifact_status_check;
ALTER TABLE artifact ADD CONSTRAINT artifact_status_check
    CHECK (status IN ('GESTATING', 'MANIFESTED', 'CANCELLED', 'FAILED'));

-- Add error_message column for failed artifacts
ALTER TABLE artifact ADD COLUMN IF NOT EXISTS error_message text;

-- Also add PARTIALLY_SUCCESSFUL to task_execution status if not already there
ALTER TABLE task_execution DROP CONSTRAINT IF EXISTS task_execution_status_check;
ALTER TABLE task_execution ADD CONSTRAINT task_execution_status_check
    CHECK (status IN ('QUEUED', 'RUNNING', 'REQUIRES_ACTION', 'COMPLETED',
                      'PARTIALLY_SUCCESSFUL', 'FAILED', 'CANCELLED'));
