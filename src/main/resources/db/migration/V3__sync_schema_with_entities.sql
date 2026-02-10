-- Rename columns to match JPA entities
ALTER TABLE artifact RENAME COLUMN content TO inline_content;
ALTER TABLE task_execution RENAME COLUMN message TO commit_message;

-- Drop unused columns from artifact table
ALTER TABLE artifact DROP COLUMN IF EXISTS mime;
ALTER TABLE artifact DROP COLUMN IF EXISTS subtitle;
ALTER TABLE artifact DROP COLUMN IF EXISTS alt;
